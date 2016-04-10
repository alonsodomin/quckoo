package io.quckoo.cluster.scheduler

import java.util.UUID

import akka.actor._
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.ShardRegion
import akka.persistence.{PersistentActor, RecoveryCompleted}

import io.quckoo.cluster.topics
import io.quckoo.fault.{ExceptionThrown, Fault}
import io.quckoo.id._
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.time.{DateTime, TimeSource}
import io.quckoo.{ExecutionPlan, JobSpec, Task, Trigger}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object ExecutionDriver {

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
  private[scheduler] final case class Created(
      jobId: JobId, spec: JobSpec, planId: PlanId,
      trigger: Trigger, executionProps: Props,
      time: DateTime
  )
  private final case class ScheduleTask(time: DateTime)
  private case object FinishPlan

  // Public execution driver state
  object DriverState {
    private[scheduler] def apply(created: Created)(implicit timeSource: TimeSource): DriverState = DriverState(
      ExecutionPlan(created.jobId, created.planId, created.trigger, created.time),
      created.spec,
      created.executionProps,
      Vector.empty
    )
  }
  final case class DriverState(
      plan: ExecutionPlan,
      jobSpec: JobSpec,
      executionProps: Props,
      completedTasks: Vector[TaskId]
  )(implicit timeSource: TimeSource) {

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

          case TaskTriggered(`jobId`, `planId`, taskId) if plan.currentTaskId.contains(taskId) =>
            copy(plan = plan.copy(
              lastTriggeredTime = Some(timeSource.currentDateTime)
            ))

          case TaskCompleted(`jobId`, `planId`, taskId, outcome) if plan.currentTaskId.contains(taskId) =>
            copy(plan = plan.copy(
              currentTaskId = None,
              lastExecutionTime = Some(timeSource.currentDateTime),
              lastOutcome = outcome),
              completedTasks = completedTasks :+ taskId
            )

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
  import ShardRegion.Passivate
  import SupervisorStrategy._

  private[this] val mediator = DistributedPubSub(context.system).mediator
  private[this] val triggerDispatcher = context.system.dispatchers.lookup("quckoo.trigger-dispatcher")
  private[this] val taskQueue = context.actorSelection(
    RootActorPath(self.path.address) / "user" / "quckoo" / "scheduler" / "queue"
  )

  private[this] var stateDuringRecovery: Option[DriverState] = None

  override def persistenceId = "ExecutionDriver-" + self.path.name

  override def preStart(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(topics.Registry, self)

  override def receiveRecover: Receive = {
    case create: Created =>
      log.debug("Execution driver recreated for plan: {}", create.planId)
      stateDuringRecovery = Some(DriverState(create))

    case event: SchedulerEvent =>
      log.debug("Execution driver event replayed. event={}", event)
      stateDuringRecovery = stateDuringRecovery.map(_.updated(event))

    case RecoveryCompleted =>
      stateDuringRecovery.foreach { st =>
        log.debug("Execution driver recovery finished. state={}")
        context.become(active(st))
        /*st.plan.currentTaskId.foreach { _ =>
          self ! ScheduleTask(timeSource.currentDateTime)
        }*/
      }
      stateDuringRecovery = None
  }

  override def receiveCommand: Receive = initial()

  private def activatePlan(state: DriverState): Unit = {
    log.info("Activating execution plan. planId={}", state.planId)
    persist(ExecutionPlanStarted(state.jobId, state.planId)) { event =>
      mediator ! DistributedPubSubMediator.Publish(topics.Scheduler, event)
      self ! scheduleOrFinish(state)
      context.become(active(state))
    }
  }

  private def initial(subscribed: Boolean = false, state: Option[DriverState] = None): Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      if (state.isDefined) {
        activatePlan(state.get)
      } else {
        context.become(initial(subscribed = true, state))
      }

    case GetExecutionPlan(_) =>
      sender() ! None

    case cmd: New =>
      val created = Created(cmd.jobId, cmd.spec, cmd.planId, cmd.trigger,
          cmd.executionProps, timeSource.currentDateTime)
      persist(created) { evt =>
        val st = DriverState(evt)
        log.debug("Creating new execution plan. planId={}", st.planId)

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
      sender() ! Some(state.plan)

    case Execution.Triggered =>
      state.plan.currentTaskId.foreach { taskId =>
        persist(TaskTriggered(state.jobId, state.planId, taskId)) { event =>
          context.become(active(state.updated(event), None))
        }
      }

    case Execution.Result(outcome) =>
      state.plan.currentTaskId.foreach { taskId =>
        persist(TaskCompleted(state.jobId, state.planId, taskId, outcome)) { event =>
          log.debug("Task finished. taskId={}", taskId)
          mediator ! DistributedPubSubMediator.Publish(topics.Scheduler, event)

          val newState = state.updated(event)
          context.become(active(newState))
          self ! nextCommand(newState, taskId, outcome)
        }
      }

    case ScheduleTask(time) =>
      lazy val delay = {
        val now = timeSource.currentDateTime
        if (time.isBefore(now) || time.isEqual(now)) 0 millis
        else (time - now).toMillis millis
      }

      def scheduleTask(task: Task): Cancellable = {
        // Schedule a new execution instance
        log.info("Scheduling a new execution. jobId={}, planId={}, taskId={}", state.jobId, state.planId, task.id)
        val execution = context.actorOf(state.executionProps, task.id.toString)
        //context.watch(execution)

        implicit val dispatcher = triggerDispatcher
        log.debug("Task {} in plan {} will be triggered after {}", task.id, state.planId, delay)
        context.system.scheduler.scheduleOnce(delay, execution, Execution.WakeUp(task, taskQueue))
      }

      // Create a new task
      val taskId = UUID.randomUUID()
      val task = Task(taskId, state.jobSpec.artifactId, Map.empty, state.jobSpec.jobClass)

      // Create a trigger to fire the task
      val internalTrigger = scheduleTask(task)
      persist(TaskScheduled(state.jobId, state.planId, taskId)) { event =>
        mediator ! DistributedPubSubMediator.Publish(topics.Scheduler, event)
        context.become(active(state.updated(event), Some(internalTrigger)))
      }

    case FinishPlan =>
      if (triggerTask.isDefined) {
        log.debug("Cancelling trigger for execution plan. planId={}", state.planId)
        triggerTask.foreach(_.cancel())
      }

      log.info("Stopping execution plan. planId={}", state.planId)
      persist(ExecutionPlanFinished(state.jobId, state.planId)) { event =>
        mediator ! DistributedPubSubMediator.Publish(topics.Scheduler, event)
        mediator ! DistributedPubSubMediator.Unsubscribe(topics.Registry, self)
        context.become(shuttingDown(state.updated(event)))
      }
  }


  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: DeathPactException => Stop
    case cause: Exception =>
      log.error(cause, "Error thrown from the execution state manager")
      Restart
  }

  private def shuttingDown(state: DriverState): Receive = {
    case GetExecutionPlan(_) =>
      sender() ! Some(state)

    case DistributedPubSubMediator.UnsubscribeAck(_) =>
      context.parent ! Passivate(stopMessage = PoisonPill)

    case _ =>
      log.warning("Execution plan '{}' has finished, shutting down.", state.planId)
  }

  private def scheduleOrFinish(state: DriverState) =
    state.plan.nextExecutionTime.map(ScheduleTask).getOrElse(FinishPlan)

  private def nextCommand(state: DriverState, lastTaskId: TaskId, outcome: Task.Outcome) = {
    if (state.plan.trigger.isRecurring) {
      outcome match {
        case Task.Success =>
          scheduleOrFinish(state)

        case Task.Failure(cause) =>
          if (shouldRetry(cause)) {
            // TODO improve retry process
            scheduleOrFinish(state)
          } else {
            log.warning("Failed task {} won't be retried.", lastTaskId)
            FinishPlan
          }

        case _ => FinishPlan
      }
    } else FinishPlan
  }

  private def shouldRetry(cause: Fault): Boolean = cause match {
    case ex: ExceptionThrown => false
    case _                   => true
  }

}
