package io.chronos.scheduler.internal.fun

import java.time.ZonedDateTime
import java.util.{Comparator, Map => JMap}

import com.hazelcast.client.HazelcastClient
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.query.Predicate
import io.chronos.Schedule
import io.chronos.id.ScheduleId
import io.chronos.scheduler.internal.ClockParser
import io.chronos.scheduler.internal.cache.{CacheAccessors, CacheStructures}

/**
 * Created by aalonsodominguez on 05/08/15.
 */
object AliveSchedulesQuery {

  private var _clientName: Option[String] = None

}

abstract class AliveSchedulesQuery(clockDef: String) extends Predicate[ScheduleId, Schedule]
  with Comparator[JMap.Entry[ScheduleId, Schedule]]
  with CacheStructures
  with CacheAccessors {

  import AliveSchedulesQuery._

  def clientName: String = _clientName.get

  def grid: HazelcastInstance = {
    val client = _clientName.map(HazelcastClient.getHazelcastClientByName)
    client.getOrElse {
      val newClient = HazelcastClient.newHazelcastClient()
      _clientName = Some(newClient.getName)
      newClient
    }
  }

  @transient
  implicit lazy val clock = ClockParser(clockDef)
  private lazy val triggerTimeCache = grid.getMap[ScheduleId, ZonedDateTime]("triggerTime")

  override def apply(mapEntry: JMap.Entry[ScheduleId, Schedule]): Boolean = {
    nextExecutionTime(mapEntry.getKey) match {
      case Some(time) =>
        triggerTimeCache.put(mapEntry.getKey, time)
        filterSchedule(mapEntry.getKey, mapEntry.getValue, time)
      case _ => false
    }
  }

  override def compare(o1: JMap.Entry[ScheduleId, Schedule], o2: JMap.Entry[ScheduleId, Schedule]): Int =
    (for {
      time1 <- Option(triggerTimeCache.get(o1.getKey))
      time2 <- Option(triggerTimeCache.get(o2.getKey))
    } yield time1.compareTo(time2)).get

  def filterSchedule(scheduleId: ScheduleId, schedule: Schedule, zonedDateTime: ZonedDateTime): Boolean

}
