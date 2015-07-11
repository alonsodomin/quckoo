package io.chronos

import java.time.ZonedDateTime

import io.chronos.id._

/**
 * Created by aalonsodominguez on 11/07/15.
 */
object Execution {

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

case class Execution(id: ExecutionId, statusHistory: List[Execution.Status]) extends Serializable {
  import Execution._

  def executionId = id

  def scheduleId: ScheduleId = executionId._1

  def status: Status = statusHistory.head

  def lastStatusChange: ZonedDateTime = status.when

  def outcome: Option[Outcome] = status match {
    case Finished(_, _, outcome) => Some(outcome)
    case _                       => None
  }

}
