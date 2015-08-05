package io.chronos.scheduler.internal.cache

import com.hazelcast.core.HazelcastInstance
import io.chronos.id._
import io.chronos.{Execution, Schedule}

/**
 * Created by aalonsodominguez on 04/08/15.
 */
trait CacheStructures {
  def grid: HazelcastInstance

  protected lazy val scheduleCounter = grid.getAtomicLong("scheduleCounter")
  protected lazy val scheduleMap = grid.getMap[ScheduleId, Schedule]("scheduleMap")

  protected lazy val executionCounter = grid.getAtomicLong("executionCounter")
  protected lazy val executionMap = grid.getMap[ExecutionId, Execution]("executions")
  protected lazy val executionsBySchedule = grid.getMap[ScheduleId, List[ExecutionId]]("executionsBySchedule")

}
