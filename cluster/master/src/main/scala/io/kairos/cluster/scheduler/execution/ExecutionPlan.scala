package io.kairos.cluster.scheduler.execution

import java.util.UUID

import akka.actor._
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
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

  case class StartPlan(jobId: JobId, spec: JobSpec)
  private case class ScheduleTask(time: DateTime)
  private case object FinishPlan

  private case class PlanState private (
      planId: PlanId,
      trigger: Trigger,
      job: Option[(JobId, JobSpec)] = None,
      startedTime: DateTime,
      currentTaskId: Option[TaskId] = None,
      lastOutcome: Option[Task.Outcome] = None,
      lastScheduledTime: Option[DateTime] = None,
      lastExecutionTime: Option[DateTime] = None,
      finishedTime: Option[DateTime] = None
  )(implicit timeSource: TimeSource) {
    import SchedulerProtocol._

    def ready: Boolean = job.isDefined && finishedTime.isEmpty

    def finished: Boolean = finishedTime.isDefined

    def nextExecutionTime: Option[DateTime] = {
      import Trigger._

      val referenceTime = lastExecutionTime match {
        case Some(time) => LastExecutionTime(time)
        case None       => ScheduledTime(lastScheduledTime.getOrElse(startedTime))
      }
      trigger.nextExecutionTime(referenceTime)
    }

    def scheduleOrFinish = nextExecutionTime.map(ScheduleTask).getOrElse(FinishPlan)

    def updated(event: SchedulerEvent): PlanState = {
      if (finished) this
      else {
        event match {
          case ExecutionPlanStarted(jobId, `planId`, spec) =>
            copy(job = Some(jobId -> spec))

          case TaskScheduled(_, `planId`, taskId) if currentTaskId.isEmpty =>
            copy(currentTaskId = Some(taskId), lastScheduledTime = Some(timeSource.currentDateTime))

          case TaskCompleted(_, `planId`, taskId, outcome) =>
            copy(currentTaskId = None, lastExecutionTime = Some(timeSource.currentDateTime), lastOutcome = Some(outcome))

          case ExecutionPlanFinished(_, `planId`) =>
            copy(currentTaskId = None, finishedTime = Some(timeSource.currentDateTime))

          case _ => this
        }
      }
    }

  }

  def props(planId: PlanId, trigger: Trigger)(executionProps: ExecutionProps)(implicit timeSource: TimeSource) =
    Props(classOf[ExecutionPlan], planId, trigger, executionProps, timeSource)

}

class ExecutionPlan(val planId: PlanId, trigger: Trigger, executionProps: ExecutionProps)(implicit timeSource: TimeSource)
    extends PersistentActor with ActorLogging {

  import ExecutionPlan._
  import RegistryProtocol._
  import SchedulerProtocol._

  private[this] val mediator = DistributedPubSub(context.system).mediator

  private[this] var triggerTask: Option[Cancellable] = None
  private[this] var state = PlanState(
    planId, trigger, startedTime = timeSource.currentDateTime
  )

  override def persistenceId = planId.toString

  override def preStart(): Unit = {
    mediator ! DistributedPubSubMediator.Subscribe(RegistryTopic, self)
    context.setReceiveTimeout(5 seconds)
  }

  override def postStop(): Unit =
    mediator ! DistributedPubSubMediator.Unsubscribe(RegistryTopic, self)

  override def receiveRecover: Receive = {
    case evt @ ExecutionPlanFinished(_, id) if state.planId == id =>
      state = state.updated(evt)

  }

  override def receiveCommand: Receive = {
    case StartPlan(jobId, jobSpec) =>
      persist(ExecutionPlanStarted(jobId, planId, jobSpec)) { event =>
        state = state.updated(event)
        mediator ! DistributedPubSubMediator.Publish(SchedulerTopic, event)
        self ! state.scheduleOrFinish
        context.become(active(jobId, jobSpec))
      }

    case ReceiveTimeout =>
      log.warning("")
  }

  private def active(jobId: JobId, jobSpec: JobSpec): Receive = {
    case JobDisabled(id) if id == jobId =>
      log.info("Job has been disabled. jobId={}", id)
      self ! FinishPlan

    case Execution.Result(`planId`, taskId, outcome) =>
      def nextCommand = {
        if (trigger.isRecurring) {
          outcome match {
            case Task.Success(_) =>
              state.scheduleOrFinish

            case Task.Failure(cause) =>
              if (shouldRetry(cause)) {
                state.scheduleOrFinish
              } else {
                FinishPlan
              }

            case _ => FinishPlan
          }
        } else FinishPlan
      }

      persist(TaskCompleted(jobId, planId, taskId, outcome)) { event =>
        state = state.updated(event)
        self ! nextCommand
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
      log.info("Scheduling a new execution. jobId={}, taskId={}", jobId, taskId)
      val execution = context.actorOf(executionProps(taskId, jobSpec), "exec-" + taskId)
      triggerTask = Some(context.system.scheduler.scheduleOnce(delay, execution, Execution.WakeUp))

      persist(TaskScheduled(jobId, planId, taskId)) { event =>
        mediator ! DistributedPubSubMediator.Publish(SchedulerTopic, event)
      }

    case FinishPlan =>
      if (triggerTask.isDefined) {
        log.debug("Cancelling trigger for execution plan. planId={}", planId)
        triggerTask.foreach(_.cancel())
        triggerTask = None
      }

      log.info("Stopping execution plan. planId={}", planId)
      persist(ExecutionPlanFinished(jobId, planId)) { event =>
        state = state.updated(event)
        mediator ! DistributedPubSubMediator.Publish(SchedulerTopic, event)
        self ! PoisonPill
        context.become(shuttingDown)
      }
  }

  private def shuttingDown: Receive = {
    case _ =>
      log.warning("Execution plan '{}' has finished, shutting down.", planId)
  }

  private def shouldRetry(cause: Faults): Boolean =
    !cause.list.exists(_.isInstanceOf[ExceptionThrown])

}
