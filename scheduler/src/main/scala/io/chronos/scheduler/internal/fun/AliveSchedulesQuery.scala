package io.chronos.scheduler.internal.fun

import java.time.ZonedDateTime
import java.util.{Comparator, Map => JMap}

import com.hazelcast.client.HazelcastClient
import com.hazelcast.query.Predicate
import io.chronos.id.ScheduleId
import io.chronos.scheduler.internal.cache.{CacheAccessors, CacheStructures}
import io.chronos.{Execution, Schedule}

/**
 * Created by aalonsodominguez on 05/08/15.
 */
abstract class AliveSchedulesQuery extends Predicate[ScheduleId, Schedule]
  with Comparator[JMap.Entry[ScheduleId, Schedule]]
  with CacheStructures with CacheAccessors {

  @transient
  lazy val hazelcastInstance = HazelcastClient.newHazelcastClient()

  private var triggerTimeCache = Map.empty[ScheduleId, ZonedDateTime]

  override def apply(mapEntry: JMap.Entry[ScheduleId, Schedule]): Boolean = {
    nextExecutionTime(mapEntry.getKey) match {
      case Some(time) =>
        triggerTimeCache += (mapEntry.getKey -> time)
        filterSchedule(mapEntry.getKey, mapEntry.getValue, time)
      case _ => false
    }
  }

  override def compare(o1: JMap.Entry[ScheduleId, Schedule], o2: JMap.Entry[ScheduleId, Schedule]): Int =
    (for {
      time1 <- triggerTimeCache.get(o1.getKey)
      time2 <- triggerTimeCache.get(o2.getKey)
    } yield time1.compareTo(time2)).get

  def filterSchedule(scheduleId: ScheduleId, schedule: Schedule, zonedDateTime: ZonedDateTime): Boolean

  def ready(scheduleId: ScheduleId): Boolean = (for {
    schedule <- Option(scheduleMap.get(scheduleId))
    execution <- currentExecutionOf(scheduleId).flatMap(getExecution)
  } yield execution is Execution.Ready).getOrElse(false)

}
