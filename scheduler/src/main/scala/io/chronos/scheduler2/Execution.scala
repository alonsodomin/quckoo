package io.chronos.scheduler2

import java.time.{Clock, ZonedDateTime}
import java.util.UUID

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import io.chronos.JobSpec
import io.chronos.id.WorkerId

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Execution {

  type ExecutionId = UUID
  
  def props(scheduleId: UUID, jobSpec: JobSpec, params: Map[String, AnyVal], timeout: Option[FiniteDuration])(implicit clock: Clock) =
    Props(classOf[Execution], scheduleId, jobSpec, params, timeout, clock)

  case object ScheduleNow
  case object TriggerNow

  sealed trait LifecycleState {
    val when: ZonedDateTime
  }

  case class Scheduled(when: ZonedDateTime) extends LifecycleState
  case class Triggered(when: ZonedDateTime) extends LifecycleState
  case class Started(when: ZonedDateTime, workerId: WorkerId) extends LifecycleState
  

}

class Execution(scheduleId: UUID,
                jobSpec: JobSpec,
                params: Map[String, AnyVal],
                timeout: Option[FiniteDuration])(implicit clock: Clock)
  extends PersistentActor with ActorLogging {
  
  import Execution._

  private val executionId: ExecutionId = UUID.randomUUID()
  private var lifecycle: List[LifecycleState] = Nil

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
