package io.chronos.scheduler

import java.time.Clock

import io.chronos.id._
import io.chronos.{Execution, JobSchedule, JobSpec}

/**
 * Created by aalonsodominguez on 01/08/15.
 */
trait ExecutionPlan {

  def getSchedule(scheduleId: ScheduleId): Option[JobSchedule]

  def getScheduledJobs: Seq[(ScheduleId, JobSchedule)]

  def getExecution(executionId: ExecutionId): Option[Execution]

  def getExecutions(filter: Execution => Boolean): Seq[Execution]

  def schedule(jobSchedule: JobSchedule): Execution

  def reschedule(scheduleId: ScheduleId): Execution

  def hasPendingExecutions: Boolean

  def takePending(f: (ExecutionId, JobSchedule, JobSpec) => Unit): Unit

  def sweepOverdueExecutions(batchLimit: Int)(f: ExecutionId => Unit)(implicit clock: Clock): Unit

  def updateExecution[T](executionId: ExecutionId, stage: Execution.Stage)(f: Execution => T): T

}
