package io.kairos.cluster.scheduler.execution

import java.util.UUID

import akka.actor._
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.ShardRegion
import akka.persistence.PersistentActor
import io.kairos.fault.{ExceptionThrown, Faults}
import io.kairos.id._
import io.kairos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.kairos.time.{DateTime, TimeSource}
import io.kairos.{Task, JobSpec, Trigger}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object ExecutionPlan {

  final val ShardName      = "ExecutionPlan"
  final val NumberOfShards = 100

  val idExtractor: ShardRegion.ExtractEntityId = {
    case n: New => (n.planId.toString, n)
    case g: Get => (g.planId.toString, g)
  }

  val shardResolver: ShardRegion.ExtractShardId = {
    case New(_, _, planId, _, _) => (planId.hashCode() % NumberOfShards).toString
    case Get(planId)             => (planId.hashCode() % NumberOfShards).toString
  }

  final case class New(jobId: JobId, spec: JobSpec, planId: PlanId, trigger: Trigger, executionProps: ExecutionProps)
  final case class Get(planId: PlanId)

  // Private messages, used for managing the internal lifecycle
  private[execution] case class Created(cmd: New, time: DateTime)
  private case class ScheduleTask(time: DateTime)
  private case object FinishPlan

  object PlanState {
    private[execution] def apply(created: Created)(implicit timeSource: TimeSource): PlanState = PlanState(
      created.cmd.jobId,
      created.cmd.spec,
      created.cmd.planId,
      created.cmd.trigger,
      created.cmd.executionProps,
      created.time
    )
  }
  final case class PlanState(
      jobId: JobId,
      jobSpec: JobSpec,
      planId: PlanId,
      trigger: Trigger,
      executionProps: ExecutionProps,
      createdTime: DateTime,
      currentTaskId: Option[TaskId] = None,
      lastOutcome: Option[Task.Outcome] = None,
      lastScheduledTime: Option[DateTime] = None,
      lastExecutionTime: Option[DateTime] = None,
      finishedTime: Option[DateTime] = None
  )(implicit timeSource: TimeSource) {
    import SchedulerProtocol._

    def finished: Boolean = finishedTime.isDefined

    def nextExecutionTime: Option[DateTime] = {
      import Trigger._

      val referenceTime = lastExecutionTime match {
        case Some(time) => LastExecutionTime(time)
        case None       => ScheduledTime(lastScheduledTime.getOrElse(createdTime))
      }
      trigger.nextExecutionTime(referenceTime)
    }

    def updated(event: SchedulerEvent): PlanState = {
      if (finished) this
      else {
        event match {
          case TaskScheduled(`jobId`, `planId`, taskId) if currentTaskId.isEmpty =>
            copy(currentTaskId = Some(taskId), lastScheduledTime = Some(timeSource.currentDateTime))

          case TaskCompleted(`jobId`, `planId`, taskId, outcome) if currentTaskId.contains(taskId) =>
            copy(currentTaskId = None, lastExecutionTime = Some(timeSource.currentDateTime), lastOutcome = Some(outcome))

          case ExecutionPlanFinished(`jobId`, `planId`) =>
            copy(currentTaskId = None, finishedTime = Some(timeSource.currentDateTime))

          case _ => this
        }
      }
    }

  }

  def props(implicit timeSource: TimeSource) =
    Props(classOf[ExecutionPlan], timeSource)

}

class ExecutionPlan(implicit timeSource: TimeSource)
    extends PersistentActor with ActorLogging {

  import ExecutionPlan._
  import RegistryProtocol._
  import SchedulerProtocol._
  import ShardRegion.Passivate

  private[this] val mediator = DistributedPubSub(context.system).mediator

  override def persistenceId = "ExecutionPlan-" + self.path.name

  override def preStart(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(RegistryTopic, self)

  override def postStop(): Unit =
    mediator ! DistributedPubSubMediator.Unsubscribe(RegistryTopic, self)

  override def receiveRecover = replaying()

  def replaying(state: Option[PlanState] = None): Receive = {
    case create: Created =>
      context.become(replaying(Some(PlanState(create))))

    case event: SchedulerEvent =>
      context.become(replaying(state.map(_.updated(event))))
  }

  override def receiveCommand: Receive = initial()

  private def initial(subscribed: Boolean = false, state: Option[PlanState] = None): Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      if (state.isDefined) {
        val st = state.get
        log.info("Activating execution plan. planId={}", st.planId)
        self ! scheduleOrFinish(st)
        context.become(active(st))
      } else {
        context.become(initial(subscribed = true, state))
      }

    case cmd: New =>
      persist(Created(cmd, timeSource.currentDateTime)) { evt =>
        val st = PlanState(evt)
        log.info("Creating new execution plan. planId={}", st.planId)
        mediator ! DistributedPubSubMediator.Publish(
            SchedulerTopic, ExecutionPlanStarted(st.jobId, st.planId)
        )
        if (subscribed) {
          self ! scheduleOrFinish(st)
          context.become(active(st))
        } else {
          context.become(initial(subscribed, state = Some(st)))
        }
      }
  }

  private def active(state: PlanState, triggerTask: Option[Cancellable] = None): Receive = {
    case JobDisabled(id) if id == state.jobId =>
      log.info("Job has been disabled, finishing execution plan. jobId={}, planId={}", id, state.planId)
      self ! FinishPlan

    case Get(_) =>
      sender() ! state

    case Execution.Result(outcome) =>
      state.currentTaskId.foreach { taskId =>
        persist(TaskCompleted(state.jobId, state.planId, taskId, outcome)) { event =>
          log.debug("Task finished. taskId={}", taskId)
          mediator ! DistributedPubSubMediator.Publish(SchedulerTopic, event)

          val newState = state.updated(event)
          context.become(active(newState))
          self ! nextCommand(newState, outcome)
        }
      }

    case ScheduleTask(time) =>
      import context.dispatcher

      val delay = {
        val now = timeSource.currentDateTime
        if (time.isBefore(now) || time.isEqual(now)) 0 millis
        else {
          val diff = now.diff(time)
          diff.toMillis millis
        }
      }

      // Create a new execution
      val taskId = UUID.randomUUID()
      log.info("Scheduling a new execution. jobId={}, planId={}, taskId={}", state.jobId, state.planId, taskId)
      val execution = context.actorOf(state.executionProps(taskId, state.jobSpec), "exec-" + taskId)
      val internalTrigger = context.system.scheduler.scheduleOnce(delay, execution, Execution.WakeUp)

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

  private def scheduleOrFinish(state: PlanState) =
    state.nextExecutionTime.map(ScheduleTask).getOrElse(FinishPlan)

  private def nextCommand(state: PlanState, outcome: Task.Outcome) = {
    if (state.trigger.isRecurring) {
      outcome match {
        case Task.Success(_) =>
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

  private def shuttingDown(state: PlanState): Receive = {
    case _ =>
      log.warning("Execution plan '{}' has finished, shutting down.", state.planId)
  }

  private def shouldRetry(cause: Faults): Boolean =
    !cause.list.exists(_.isInstanceOf[ExceptionThrown])

}
