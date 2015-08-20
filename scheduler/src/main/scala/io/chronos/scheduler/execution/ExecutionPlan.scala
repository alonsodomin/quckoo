package io.chronos.scheduler.execution

import java.time.{Clock, Duration => JDuration, ZonedDateTime}
import java.util.UUID

import akka.actor._
import io.chronos.Trigger._
import io.chronos.id._
import io.chronos.scheduler.Registry.{JobDisabled, JobNotEnabled}
import io.chronos.{JobSpec, Trigger}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object ExecutionPlan {

  def props(trigger: Trigger)(executionProps: ExecutionProps)(implicit clock: Clock) =
    Props(classOf[ExecutionPlan], trigger, executionProps, clock)

}

class ExecutionPlan(trigger: Trigger, executionProps: ExecutionProps)(implicit clock: Clock)
  extends Actor with ActorLogging {

  private val planId: PlanId = UUID.randomUUID()
  private var triggerTask: Option[Cancellable] = None
  private val scheduledTime = ZonedDateTime.now(clock)
  private var lastExecutionTime: Option[ZonedDateTime] = None

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[JobDisabled])
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
  }

  override def receive: Receive = {
    case jobSpec: JobSpec =>
      context.become(schedule(jobSpec.id, jobSpec))

    case JobNotEnabled(jobId) =>
      log.info("Given job can't be scheduled as it is not enabled. jobId={}", jobId)
      context.become(shutdown)
  }

  private def active(jobId: JobId, jobSpec: JobSpec): Receive = {
    case JobDisabled(id) if id == jobId =>
      log.info("Job has been disabled. jobId={}", id)
      context.become(shutdown)

    case Execution.Result(outcome) =>
      lastExecutionTime = Some(ZonedDateTime.now(clock))
      if (trigger.isRecurring) {
        outcome match {
          case _: Execution.Success =>
            context.become(schedule(jobId, jobSpec))

          case _ =>
            // Plan is no longer needed
            context.become(shutdown)
        }
      }
  }

  private def shutdown: Receive = {
    if (triggerTask.isDefined) {
      log.info("Stopping trigger for current execution plan. planId={}", planId)
      triggerTask.foreach( _.cancel() )
      triggerTask = None
    }

    log.info("Stopping execution plan. planId={}", planId)
    self ! PoisonPill

    { case _ => log.error("Execution plan unavailable, shutting down.") }
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
        log.info("Scheduling a new execution for job {}", jobId)
        val execution = context.actorOf(executionProps(planId, jobSpec))
        triggerTask = Some(context.system.scheduler.scheduleOnce(delay, execution, Execution.WakeUp))

        active(jobId, jobSpec)

      case _ => shutdown
    }
  }

}
