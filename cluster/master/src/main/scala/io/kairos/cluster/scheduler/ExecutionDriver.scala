package io.kairos.cluster.scheduler

import java.util.UUID

import akka.actor._
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.ShardRegion
import akka.persistence.{PersistentActor, RecoveryCompleted}
import io.kairos.fault.{ExceptionThrown, Fault}
import io.kairos.id._
import io.kairos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.kairos.time.{DateTime, TimeSource}
import io.kairos.{ExecutionPlan, JobSpec, Task, Trigger}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object ExecutionDriver {
  import SchedulerProtocol._

  final val ShardName      = "ExecutionDriver"
  final val NumberOfShards = 100

  val idExtractor: ShardRegion.ExtractEntityId = {
    case n: New              => (n.planId.toString, n)
    case g: GetExecutionPlan => (g.planId.toString, g)
  }

  val shardResolver: ShardRegion.ExtractShardId = {
    case New(_, _, planId, _, _)  => (planId.hashCode() % NumberOfShards).toString
    case GetExecutionPlan(planId) => (planId.hashCode() % NumberOfShards).toString
  }

  // Only for internal usage from the Scheduler actor
  private[scheduler] case class New(
      jobId: JobId, spec: JobSpec, planId: PlanId,
      trigger: Trigger, executionProps: Props
  )

  // Private messages, used for managing the internal lifecycle
  private[scheduler] case class Created(
      jobId: JobId, spec: JobSpec, planId: PlanId,
      trigger: Trigger, executionProps: Props,
      time: DateTime
  )
  private case class ScheduleTask(time: DateTime)
  private case object FinishPlan

  // Public execution plan state
  object DriverState {
    private[scheduler] def apply(created: Created)(implicit timeSource: TimeSource): DriverState = DriverState(
      ExecutionPlan(created.jobId, created.planId, created.trigger, created.time),
      created.spec,
      created.executionProps
    )
  }
  final case class DriverState(
      plan: ExecutionPlan,
      jobSpec: JobSpec,
      executionProps: Props
  )(implicit timeSource: TimeSource) {
    import SchedulerProtocol._

    val jobId = plan.jobId
    val planId = plan.planId

    def updated(event: SchedulerEvent): DriverState = {
      if (plan.finished) this
      else {
        event match {
          case TaskScheduled(`jobId`, `planId`, taskId) if plan.currentTaskId.isEmpty =>
            copy(plan = plan.copy(
              currentTaskId = Some(taskId),
              lastScheduledTime = Some(timeSource.currentDateTime)
            ))

          case TaskCompleted(`jobId`, `planId`, taskId, outcome) if plan.currentTaskId.contains(taskId) =>
            copy(plan = plan.copy(
              currentTaskId = None,
              lastExecutionTime = Some(timeSource.currentDateTime),
              lastOutcome = outcome
            ))

          case ExecutionPlanFinished(`jobId`, `planId`) =>
            copy(plan = plan.copy(
              currentTaskId = None,
              finishedTime = Some(timeSource.currentDateTime)
            ))

          case _ => this
        }
      }
    }

  }

  def props(implicit timeSource: TimeSource) =
    Props(classOf[ExecutionDriver], timeSource)

}

class ExecutionDriver(implicit timeSource: TimeSource)
    extends PersistentActor with ActorLogging {

  import ExecutionDriver._
  import RegistryProtocol._
  import SchedulerProtocol._
  import ShardRegion.Passivate

  private[this] val mediator = DistributedPubSub(context.system).mediator
  private[this] val triggerDispatcher = context.system.dispatchers.lookup("kairos.trigger-dispatcher")
  private[this] val taskQueue = context.actorSelection(
    RootActorPath(self.path.address) / "user" / "kairos" / "scheduler" / "queue"
  )

  override def persistenceId = "ExecutionPlan-" + self.path.name

  override def preStart(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(RegistryTopic, self)

  override def postStop(): Unit =
    mediator ! DistributedPubSubMediator.Unsubscribe(RegistryTopic, self)

  override def receiveRecover = replaying()

  def replaying(state: Option[DriverState] = None): Receive = {
    case create: Created =>
      context.become(replaying(Some(DriverState(create))))

    case event: SchedulerEvent if state.isDefined =>
      context.become(replaying(state.map(_.updated(event))))

    case RecoveryCompleted if state.isDefined =>
      state.filterNot(_.plan.finished).
        map(active(_)).
        foreach(context.become)
  }

  override def receiveCommand: Receive = initial()

  private def activatePlan(state: DriverState): Unit = {
    log.info("Activating execution plan. planId={}", state.planId)
    self ! scheduleOrFinish(state)
    mediator ! DistributedPubSubMediator.Publish(
      SchedulerTopic, ExecutionPlanStarted(state.jobId, state.planId)
    )
    context.become(active(state))
  }

  private def initial(subscribed: Boolean = false, state: Option[DriverState] = None): Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      if (state.isDefined) {
        activatePlan(state.get)
      } else {
        context.become(initial(subscribed = true, state))
      }

    case cmd: New =>
      val created = Created(cmd.jobId, cmd.spec, cmd.planId, cmd.trigger,
          cmd.executionProps, timeSource.currentDateTime)
      persist(created) { evt =>
        val st = DriverState(evt)
        log.info("Creating new execution plan. planId={}", st.planId)

        if (subscribed) {
          activatePlan(st)
        } else {
          context.become(initial(subscribed, state = Some(st)))
        }
      }
  }

  private def active(state: DriverState, triggerTask: Option[Cancellable] = None): Receive = {
    case JobDisabled(id) if id == state.jobId =>
      log.info("Job has been disabled, finishing execution plan. jobId={}, planId={}", id, state.planId)
      self ! FinishPlan

    case GetExecutionPlan(_) =>
      sender() ! state.plan

    case Execution.Result(outcome) =>
      state.plan.currentTaskId.foreach { taskId =>
        persist(TaskCompleted(state.jobId, state.planId, taskId, outcome)) { event =>
          log.debug("Task finished. taskId={}", taskId)
          mediator ! DistributedPubSubMediator.Publish(SchedulerTopic, event)

          val newState = state.updated(event)
          context.become(active(newState))
          self ! nextCommand(newState, outcome)
        }
      }

    case ScheduleTask(time) =>
      val delay = {
        val now = timeSource.currentDateTime
        if (time.isBefore(now) || time.isEqual(now)) 0 millis
        else {
          val diff = now.diff(time)
          diff.toMillis millis
        }
      }

      def scheduleTask(task: Task): Cancellable = {
        // Schedule a new execution instance
        log.info("Scheduling a new execution. jobId={}, planId={}, taskId={}", state.jobId, state.planId, task.id)
        val execution = context.watch(context.actorOf(state.executionProps, "exec-" + task.id))

        implicit val dispatcher = triggerDispatcher
        context.system.scheduler.scheduleOnce(delay, execution, Execution.WakeUp(task, taskQueue))
      }

      // Create a new task
      val taskId = UUID.randomUUID()
      val task = Task(taskId, state.jobSpec.artifactId, Map.empty, state.jobSpec.jobClass)

      // Create a trigger to fire the task
      val internalTrigger = scheduleTask(task)
      persist(TaskScheduled(state.jobId, state.planId, taskId)) { event =>
        mediator ! DistributedPubSubMediator.Publish(SchedulerTopic, event)
        context.become(active(state.updated(event), Some(internalTrigger)))
      }

    case FinishPlan =>
      if (triggerTask.isDefined) {
        log.debug("Cancelling trigger for execution plan. planId={}", state.planId)
        triggerTask.foreach(_.cancel())
      }

      log.info("Stopping execution plan. planId={}", state.planId)
      persist(ExecutionPlanFinished(state.jobId, state.planId)) { event =>
        mediator ! DistributedPubSubMediator.Publish(SchedulerTopic, event)
        context.parent ! Passivate(stopMessage = PoisonPill)
        context.become(shuttingDown(state.updated(event)))
      }
  }

  private def scheduleOrFinish(state: DriverState) =
    state.plan.nextExecutionTime.map(ScheduleTask).getOrElse(FinishPlan)

  private def nextCommand(state: DriverState, outcome: Task.Outcome) = {
    if (state.plan.trigger.isRecurring) {
      outcome match {
        case Task.Success =>
          scheduleOrFinish(state)

        case Task.Failure(cause) =>
          if (shouldRetry(cause)) {
            // TODO improve retry process
            scheduleOrFinish(state)
          } else {
            FinishPlan
          }

        case _ => FinishPlan
      }
    } else FinishPlan
  }

  private def shuttingDown(state: DriverState): Receive = {
    case _ =>
      log.warning("Execution plan '{}' has finished, shutting down.", state.planId)
  }

  private def shouldRetry(cause: Fault): Boolean = cause match {
    case ex: ExceptionThrown => false
    case _                   => true
  }

}
