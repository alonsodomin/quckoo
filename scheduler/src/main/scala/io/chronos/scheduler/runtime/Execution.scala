package io.chronos.scheduler.runtime

import java.time.ZonedDateTime

import io.chronos.id.{ExecutionId, ScheduleId, WorkerId}

/**
 * Created by aalonsodominguez on 09/07/15.
 */

object Execution {

  def scheduled(id: ExecutionId, when: ZonedDateTime): Execution = {
    Execution(id, Scheduled(when) :: Nil)
  }

  sealed abstract class Status(val ordinal: Int) extends Ordered[Status] with Serializable {
    implicit val when: ZonedDateTime

    override def compare(that: Status): Int = {
      ordinal compareTo that.ordinal
    }
    
  }

  sealed trait Waiting extends Status
  sealed trait InProgress extends Status

  case class Scheduled(when: ZonedDateTime) extends Status(1) with Waiting
  case class Triggered(when: ZonedDateTime) extends Status(2) with Waiting with InProgress
  case class Started(when: ZonedDateTime, where: WorkerId) extends Status(3) with InProgress
  case class Finished(when: ZonedDateTime, where: WorkerId, outcome: Outcome) extends Status(4)
  
  sealed trait Outcome
  case class Success(result: Any) extends Outcome
  case class Failed(cause: Throwable) extends Outcome
  case object TimedOut extends Outcome
}

case class Execution private (id: ExecutionId, statusHistory: List[Execution.Status]) extends Serializable {
  import Execution._

  def executionId = id

  def scheduleId: ScheduleId = executionId._1

  def status: Status = statusHistory.head
  
  def lastStatusChange: ZonedDateTime = status.when

  def outcome: Option[Outcome] = status match {
    case Finished(_, _, outcome) => Some(outcome)
    case _                       => None
  }

  private[runtime] def update(status: Status): Execution = {
    require(this.status.ordinal < status.ordinal)
    copy(statusHistory = status :: statusHistory)
  }
  
}
