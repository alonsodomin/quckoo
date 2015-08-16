package io.chronos.scheduler

import java.time.{Clock, Duration => JDuration, ZonedDateTime}
import java.util.UUID

import akka.actor._
import io.chronos.Trigger._
import io.chronos.id.JobId
import io.chronos.protocol.{JobDisabled, JobNotEnabled}
import io.chronos.{JobSpec, Trigger}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Schedule {

  def props(params: Map[String, AnyVal] = Map.empty,
            trigger: Trigger = Immediate,
            timeout: Option[FiniteDuration] = None) =
    Props(classOf[Schedule], params, trigger, timeout)

}

class Schedule(params: Map[String, AnyVal], trigger: Trigger, timeout: Option[FiniteDuration])(implicit clock: Clock)
  extends Actor with ActorLogging {

  private val scheduleId = UUID.randomUUID()
  private var triggerTask: Option[Cancellable] = None
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
      triggerDelay match {
        case Some(delay) =>
          // Create a new execution and subscribe to any of its state transitions
          val execution = context.actorOf(Execution.props(scheduleId, jobSpec, params, timeout))

          triggerTask = Some(context.system.scheduler.scheduleOnce(delay, execution, Execution.TriggerNow))
          context.become(active(jobSpec.id, jobSpec))

        case _ =>
          // Arakiri time
          self ! PoisonPill
      }

    case JobNotEnabled(_) =>
      self ! PoisonPill
  }

  private def active(jobId: JobId, jobSpec: JobSpec): Receive = {
    case JobDisabled(id) if id == jobId =>
      log.info("Job has been disabled. jobId={}", id)
      triggerTask.foreach { _.cancel() }
      triggerTask = None
      self ! PoisonPill
  }

  private def triggerDelay: Option[FiniteDuration] = {
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

  private def nextExecutionTime: Option[ZonedDateTime] = trigger.nextExecutionTime(lastExecutionTime match {
    case Some(time) => LastExecutionTime(time)
    case None       => ScheduledTime(ZonedDateTime.now(clock))
  })

}
