package io.kairos.cluster.scheduler.execution

import java.time.{Clock, Duration => JDuration, ZonedDateTime}
import java.util.UUID

import akka.actor._
import io.kairos.Trigger._
import io.kairos.cluster.TaskFailureCause
import io.kairos.id._
import io.kairos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.kairos.{JobSpec, Trigger}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object ExecutionPlan {

  def props(planId: PlanId, trigger: Trigger)(executionProps: ExecutionFSMProps)(implicit clock: Clock) =
    Props(classOf[ExecutionPlan], planId, trigger, executionProps, clock)

}

class ExecutionPlan(val planId: PlanId, trigger: Trigger, executionProps: ExecutionFSMProps)(implicit clock: Clock)
  extends Actor with ActorLogging {

  import RegistryProtocol._
  import SchedulerProtocol._

  private var triggerTask: Option[Cancellable] = None
  private val scheduledTime = ZonedDateTime.now(clock)
  private var lastExecutionTime: Option[ZonedDateTime] = None

  private var originalRequestor: Option[ActorRef] = None

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[JobDisabled])
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
  }

  override def receive: Receive = {
    case (jobId: JobId, jobSpec: JobSpec) =>
      originalRequestor = Some(sender())
      context.become(schedule(jobId, jobSpec))

    case JobNotEnabled(jobId) =>
      log.info("Given job can't be scheduled as it is not enabled. jobId={}", jobId)
      context.become(shutdown)
  }

  private def active(jobId: JobId, jobSpec: JobSpec): Receive = {
    case JobDisabled(id) if id == jobId =>
      log.info("Job has been disabled. jobId={}", id)
      context.become(shutdown)

    case ExecutionFSM.Result(outcome) =>
      def nextStage: Receive = if (trigger.isRecurring) {
        outcome match {
          case _: Execution.Success =>
            schedule(jobId, jobSpec)

          case Execution.Failure(cause) =>
            if (shouldRetry(cause))
              schedule(jobId, jobSpec)
            else shutdown

          case _ =>
            // Plan is no longer needed
            shutdown
        }
      } else shutdown

      lastExecutionTime = Some(ZonedDateTime.now(clock))
      context.become(nextStage)
  }

  private def shouldRetry(cause: TaskFailureCause): Boolean = cause match {
    case Left(resolutionFailed) =>
      log.error("Worker node could not resolve dependencies for the execution. jobId={}, unresolvedDependencies={}")
      true

    case Right(exception) =>
      log.error(exception, "Execution threw exception on worker. planId={}", planId)
      false
  }

  private def shutdown: Receive = {
    if (triggerTask.isDefined) {
      log.info("Stopping trigger for current execution plan. planId={}", planId)
      triggerTask.foreach( _.cancel() )
      triggerTask = None
    }

    log.info("Stopping execution plan. planId={}", planId)
    self ! PoisonPill

    { case _ => log.error("Execution plan '{}' unavailable, shutting down.", planId) }
  }

  private def schedule(jobId: JobId, jobSpec: JobSpec): Receive = {
    def nextExecutionTime: Option[ZonedDateTime] = trigger.nextExecutionTime(lastExecutionTime match {
      case Some(time) => LastExecutionTime(time)
      case None       => ScheduledTime(scheduledTime)
    })

    def triggerDelay: Option[FiniteDuration] = {
      val now = ZonedDateTime.now(clock)
      nextExecutionTime match {
        case Some(time) if time.isBefore(now) || time.isEqual(now) =>
          Some(0 millis)
        case Some(time) =>
          val delay = JDuration.between(now, time)
          Some(delay.toMillis millis)
        case None => None
      }
    }

    triggerDelay match {
      case Some(delay) =>
        import context.dispatcher

        // Create a new execution
        val taskId = UUID.randomUUID()
        log.info("Scheduling a new execution. jobId={}, taskId={}", jobId, taskId)
        val execution = context.actorOf(executionProps(taskId, jobSpec), "exec-" + taskId)
        triggerTask = Some(context.system.scheduler.scheduleOnce(delay, execution, ExecutionFSM.WakeUp))
        originalRequestor.foreach { _ ! JobScheduled(jobId, planId) }

        active(jobId, jobSpec)

      case _ => shutdown
    }
  }

}
