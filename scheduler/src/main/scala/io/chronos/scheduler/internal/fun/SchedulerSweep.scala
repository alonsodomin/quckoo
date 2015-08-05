package io.chronos.scheduler.internal.fun

import java.time.{Clock, ZonedDateTime}

import com.hazelcast.core.HazelcastInstance
import io.chronos.Execution
import io.chronos.id._
import io.chronos.scheduler.ExecutionCache
import io.chronos.scheduler.internal.cluster.NonBlockingSync

/**
 * Created by aalonsodominguez on 04/08/15.
 */
class SchedulerSweep(hazelcastInstance: HazelcastInstance, executionCache: ExecutionCache) {

  val clusterSync = new NonBlockingSync(hazelcastInstance, "sweep")

  def apply(f: ExecutionId => Unit)(implicit clock: Clock): Unit = clusterSync.synchronize {
    // Determines if a given schedule is ready to be triggered
    def ready(scheduleId: ScheduleId): Boolean = (for {
      schedule <- executionCache.getSchedule(scheduleId)
      execution <- executionCache.currentExecutionOf(scheduleId).flatMap(executionCache.getExecution)
    } yield execution is Execution.Ready).getOrElse(false)

    val now = ZonedDateTime.now(clock)
    executionCache.aliveSchedules {
      (scheduleId, _, nextExec) => ready(scheduleId) && (nextExec.isBefore(now) || nextExec.isEqual(now))
    } map { executionCache.currentExecutionOf(_).get
    } foreach f
  }

}
