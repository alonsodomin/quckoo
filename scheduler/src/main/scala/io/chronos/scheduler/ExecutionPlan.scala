package io.chronos.scheduler

import java.time.Clock

import io.chronos.id._
import io.chronos.{Execution, Schedule}

/**
 * Created by aalonsodominguez on 01/08/15.
 */
trait ExecutionPlan {

  def getSchedule(scheduleId: ScheduleId): Option[Schedule]

  def getScheduledJobs: Seq[(ScheduleId, Schedule)]

  def getExecution(executionId: ExecutionId): Option[Execution]

  def getExecutions(filter: Execution => Boolean): Seq[Execution]

  def schedule(jobSchedule: Schedule)(implicit clock: Clock): Execution

  def reschedule(scheduleId: ScheduleId)(implicit clock: Clock): Execution

  def sweepOverdueExecutions(batchLimit: Int)(f: ExecutionId => Unit)(implicit clock: Clock): Unit

  def updateExecution[T](executionId: ExecutionId, stage: Execution.Stage)(f: Execution => T): T

}
