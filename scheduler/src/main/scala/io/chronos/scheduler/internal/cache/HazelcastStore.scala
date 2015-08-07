package io.chronos.scheduler.internal.cache

import java.time.Clock
import java.util.function.BiFunction
import java.util.{Map => JMap}

import com.hazelcast.core.HazelcastInstance
import io.chronos.id._
import io.chronos.scheduler.ExecutionCache
import io.chronos.{Execution, Schedule}
import org.slf4s.Logging

/**
 * Created by aalonsodominguez on 01/08/15.
 */
class HazelcastStore(val grid: HazelcastInstance) extends HazelcastExecutionCache
  with HazelcastRegistryCache with ExecutionCache
  with HazelcastExecutionQueue with Logging {

  override def getScheduledJobs: Seq[(ScheduleId, Schedule)] = {
    implicit val clock = Clock.systemUTC()
    val ordering: Ordering[(ScheduleId, Schedule)] = Ordering.by(p => p._2.jobId)
    DistributedQueryTraversable(scheduleMap, ordering, 50).toStream
  }

  override def schedule(jobSchedule: Schedule)(implicit clock: Clock): Execution = {
    val scheduleId = (jobSchedule.jobId, scheduleCounter.incrementAndGet())
    scheduleMap.put(scheduleId, jobSchedule)
    defineExecutionFor(scheduleId)
  }
  
  override def reschedule(scheduleId: ScheduleId)(implicit clock: Clock): Execution = defineExecutionFor(scheduleId)

  private def defineExecutionFor(scheduleId: ScheduleId)(implicit clock: Clock): Execution = {
    val executionId = (scheduleId, executionCounter.incrementAndGet())
    val execution = Execution(executionId)
    executionMap.put(executionId, execution)
    executionsBySchedule.merge(scheduleId, List(executionId), new BiFunction[List[ExecutionId], List[ExecutionId], List[ExecutionId]] {
      override def apply(oldValue: List[ExecutionId], newValue: List[ExecutionId]): List[ExecutionId] =
        if (oldValue == null || oldValue.isEmpty) newValue
        else newValue ::: oldValue
    })
    execution
  }
  
}
