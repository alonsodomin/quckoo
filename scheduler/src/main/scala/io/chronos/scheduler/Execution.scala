package io.chronos.scheduler

import java.time.ZonedDateTime

import io.chronos.id.{ExecutionId, WorkerId}
import io.chronos.scheduler.Execution.Status

/**
 * Created by aalonsodominguez on 09/07/15.
 */

object Execution {

  def scheduled(id: ExecutionId, when: ZonedDateTime): Execution = {
    Execution(id, Scheduled(when) :: Nil)
  }

  sealed abstract class Status(val ordinal: Int) extends Ordered[Status] {
    val when: ZonedDateTime

    override def compare(that: Status): Int = {
      ordinal compareTo that.ordinal
    }
    
  }
  sealed trait InitialStatus extends Status

  case class Scheduled(when: ZonedDateTime) extends Status(1) with InitialStatus
  case class Triggered(when: ZonedDateTime) extends Status(2) with InitialStatus
  case class Started(when: ZonedDateTime) extends Status(3)
  case class Finished(when: ZonedDateTime, outcome: Outcome) extends Status(4)
  
  sealed trait Outcome
  case class Success(result: Any) extends Outcome
  case class Failed(cause: Throwable) extends Outcome
  case class TimedOut(workerId: WorkerId) extends Outcome
}

case class Execution private (id: ExecutionId, statusHistory: List[Status]) {
  import Execution._

  def executionId = id

  def status: Status = statusHistory.head
  
  def lastStatusChange: ZonedDateTime = status.when
  
  def isInProgress: Boolean = status match {
    case Started(_) => true
    case _          => false
  }

  def isWaiting: Boolean = status match {
    case _: InitialStatus => true
    case _                => false
  }

  def outcome: Option[Outcome] = status match {
    case Finished(_, outcome) => Some(outcome)
    case _                    => None
  }

  def update(status: Status): Execution = {
    require(this.status.ordinal < status.ordinal)
    copy(statusHistory = status :: statusHistory)
  }
  
}
