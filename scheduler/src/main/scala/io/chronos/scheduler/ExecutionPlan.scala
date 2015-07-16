package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}

import io.chronos.id._
import io.chronos.{Execution, JobSchedule}

/**
  * Created by aalonsodominguez on 10/07/15.
  */
trait ExecutionPlan {

  def executions(filter: Execution => Boolean): Seq[Execution]

  def scheduledJobs: Seq[(ScheduleId, JobSchedule)]

  def schedule(schedule: JobSchedule)(implicit clock: Clock): Execution

  def fetchOverdueExecutions(batchSize: Int)(consumer: Execution => Unit)(implicit clock: Clock): Unit

  def lastExecutionTime(scheduleId: ScheduleId): Option[ZonedDateTime]

  def updateExecution(executionId: ExecutionId, status: Execution.Stage)(c: Execution => Unit): Unit

 }
