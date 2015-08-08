package io.chronos.scheduler.internal.cache

import java.util

import com.hazelcast.core.{HazelcastInstance, IMap}
import io.chronos.Schedule
import io.chronos.id._

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 05/08/15.
 */
object ScheduleCache {

  type ScheduleEntry = (ScheduleId, Schedule)

}

class ScheduleCache(grid: HazelcastInstance) extends DistributedCache[ScheduleId, Schedule] {

  import ScheduleCache._

  private lazy val scheduleCounter = grid.getAtomicLong("scheduleCounter")
  private lazy val mutex = grid.getLock("scheduleDispose")
  private lazy val activeScheduleMap = grid.getMap[ScheduleId, Schedule]("activeScheduleMap")
  private lazy val inactiveScheduleMap = grid.getMap[ScheduleId, Schedule]("inactiveScheduleMap")

  def active: Traversable[ScheduleEntry] =
    lazyTraversable(activeScheduleMap)

  def inactive: Traversable[ScheduleEntry] =
    lazyTraversable(inactiveScheduleMap)

  def get(scheduleId: ScheduleId): Option[Schedule] =
    Option(activeScheduleMap.get(scheduleId)).
      orElse(Option(inactiveScheduleMap.get(scheduleId)))

  def apply(scheduleId: ScheduleId): Schedule =
    get(scheduleId).get

  def +=(schedule: Schedule): ScheduleId = {
    val scheduleId = (schedule.jobId, scheduleCounter.incrementAndGet())
    activeScheduleMap.put(scheduleId, schedule)
    scheduleId
  }

  def deactivate(scheduleId: ScheduleId): Unit = {
    require(activeScheduleMap.containsKey(scheduleId))

    mutex.lock()
    try {
      val schedule = activeScheduleMap.remove(scheduleId)
      inactiveScheduleMap.put(scheduleId, schedule)
    } finally {
      mutex.unlock()
    }
  }

  def toTraversable: Traversable[ScheduleEntry] =
    Traversable.concat(active, inactive).view

  private def lazyTraversable(source: IMap[ScheduleId, Schedule]): Traversable[ScheduleEntry] = {
    val traversable = new Traversable[ScheduleEntry] {
      val values: util.Collection[util.Map.Entry[ScheduleId, Schedule]] = source.entrySet()

      override def foreach[U](f: ScheduleEntry => U) =
        for (entry <- values) f(entry.getKey, entry.getValue)
    }
    traversable.view
  }

}
