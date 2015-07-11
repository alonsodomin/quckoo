package io.chronos.scheduler

import java.time.ZonedDateTime

import io.chronos.Execution
import io.chronos.Execution._
import io.chronos.id.ExecutionId

/**
 * Created by aalonsodominguez on 09/07/15.
 */
object ScheduledExecution {

  def scheduled(id: ExecutionId, when: ZonedDateTime): ScheduledExecution = {
    ScheduledExecution(id, Scheduled(when) :: Nil)
  }

}

case class ScheduledExecution private (id: ExecutionId, statusHistory: List[Execution.Status]) extends Execution {

  def update(status: Status): Execution = {
    require(this.status.ordinal < status.ordinal)
    copy(statusHistory = status :: statusHistory)
  }
  
}
