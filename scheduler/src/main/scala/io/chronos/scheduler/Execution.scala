package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}
import java.util.UUID

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import io.chronos.JobSpec
import io.chronos.cluster.WorkerId
import io.chronos.id.{ExecutionId, ScheduleId}
import io.chronos.protocol.ExecutionFailedCause

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Execution {

  def props(scheduleId: ScheduleId, jobSpec: JobSpec, params: Map[String, AnyVal], timeout: Option[FiniteDuration])(implicit clock: Clock) =
    Props(classOf[Execution], scheduleId, jobSpec, params, timeout, clock)

  case object ScheduleNow
  case object TriggerNow

  sealed trait LifecycleEvent {
    val when: ZonedDateTime
  }

  case class Scheduled(when: ZonedDateTime) extends LifecycleEvent
  case class Triggered(when: ZonedDateTime) extends LifecycleEvent
  case class Started(when: ZonedDateTime, workerId: WorkerId) extends LifecycleEvent
  case class Finished(when: ZonedDateTime, workerId: WorkerId, outcome: Outcome) extends LifecycleEvent
  
  sealed trait Outcome
  case class Success(result: AnyVal) extends Outcome
  case class Failed(cause: ExecutionFailedCause) extends Outcome
  case object TimedOut extends Outcome
}

class Execution(scheduleId: ScheduleId,
                jobSpec: JobSpec,
                params: Map[String, AnyVal],
                timeout: Option[FiniteDuration])(implicit clock: Clock)
  extends PersistentActor with ActorLogging {
  
  import Execution._

  private val executionId: ExecutionId = UUID.randomUUID()
  private var lifecycle: List[LifecycleEvent] = Nil

  override def persistenceId: String = ???

  override def receiveRecover: Receive = ???

  override def receiveCommand: Receive = {
    case ScheduleNow =>
      persist(Scheduled(ZonedDateTime.now(clock))) { newState =>
        lifecycle = newState :: lifecycle
        context.system.eventStream.publish(newState)
      }

    case TriggerNow =>
      persist(Triggered(ZonedDateTime.now(clock))) { newState =>
        lifecycle = newState :: lifecycle
        context.system.eventStream.publish(newState)
      }
  }

}
