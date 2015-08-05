package io.chronos.scheduler

import java.time.Clock

import io.chronos.id._
import io.chronos.scheduler.internal.cache.{CacheAccessors, CacheStructures}
import io.chronos.{Execution, Schedule}

/**
 * Created by aalonsodominguez on 01/08/15.
 */
trait ExecutionCache extends CacheStructures with CacheAccessors {

  def getScheduledJobs: Seq[(ScheduleId, Schedule)]

  def getExecutions(filter: Execution => Boolean): Traversable[Execution]

  def schedule(jobSchedule: Schedule)(implicit clock: Clock): Execution

  def reschedule(scheduleId: ScheduleId)(implicit clock: Clock): Execution

  def overdueExecutions(implicit clock: Clock): Traversable[ExecutionId]

  def updateExecution[T](executionId: ExecutionId, stage: Execution.Stage)(f: Execution => T): T

}
