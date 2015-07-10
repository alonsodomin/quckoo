package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}

import io.chronos.JobSchedule
import io.chronos.id._
import io.chronos.scheduler.runtime.Execution

/**
 * Created by aalonsodominguez on 10/07/15.
 */
trait ExecutionPlan {

  def schedule(clock: Clock, schedule: JobSchedule): ExecutionId

  def fetchOverdueExecutions(clock: Clock, batchSize: Int)(implicit c: Execution => Unit): Unit

  def lastExecutionTime(scheduleId: ScheduleId): Option[ZonedDateTime]

  def updateExecution(executionId: ExecutionId, status: Execution.Status)(implicit c: Execution => Unit): Unit

}
