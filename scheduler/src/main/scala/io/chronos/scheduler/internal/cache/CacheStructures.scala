package io.chronos.scheduler.internal.cache

import com.hazelcast.core.HazelcastInstance
import io.chronos.id._
import io.chronos.{Execution, Schedule}

/**
 * Created by aalonsodominguez on 04/08/15.
 */
trait CacheStructures {
  val hazelcastInstance: HazelcastInstance

  protected lazy val scheduleCounter = hazelcastInstance.getAtomicLong("scheduleCounter")
  protected lazy val scheduleMap = hazelcastInstance.getMap[ScheduleId, Schedule]("scheduleMap")

  protected lazy val executionCounter = hazelcastInstance.getAtomicLong("executionCounter")
  protected lazy val executionMap = hazelcastInstance.getMap[ExecutionId, Execution]("executions")
  protected lazy val executionsBySchedule = hazelcastInstance.getMap[ScheduleId, List[ExecutionId]]("executionsBySchedule")

}
