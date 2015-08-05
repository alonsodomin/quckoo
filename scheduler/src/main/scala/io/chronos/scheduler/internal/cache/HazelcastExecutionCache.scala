package io.chronos.scheduler.internal.cache

import java.time.ZonedDateTime
import java.util.{Comparator, Map => JMap}

import com.hazelcast.client.HazelcastClient
import com.hazelcast.query.{PagingPredicate, Predicate}
import io.chronos.id._
import io.chronos.scheduler.ExecutionCache
import io.chronos.scheduler.internal.fun.ReferenceTimeFetch
import io.chronos.{Execution, Schedule}

/**
 * Created by aalonsodominguez on 04/08/15.
 */
trait HazelcastExecutionCache extends ExecutionCache with CacheStructures with CacheAccessors {

  def aliveSchedules(f: (ScheduleId, Schedule, ZonedDateTime) => Boolean): Traversable[(ScheduleId, Schedule)] = {
    val cachingPredicate = new Predicate[ScheduleId, Schedule]
      with Comparator[JMap.Entry[ScheduleId, Schedule]]
      with Serializable {

      @transient
      lazy val hazelcastClient = HazelcastClient.newHazelcastClient()
      var triggerTimeCache = Map.empty[ScheduleId, ZonedDateTime]

      override def apply(mapEntry: JMap.Entry[ScheduleId, Schedule]): Boolean = {
        val referentTimeFetch = new ReferenceTimeFetch(hazelcastClient)
        referentTimeFetch(mapEntry.getKey) match {
          case Some(time) =>
            triggerTimeCache += (mapEntry.getKey -> time.when)
            f(mapEntry.getKey, mapEntry.getValue, time.when)
          case _ => false
        }
      }

      override def compare(o1: JMap.Entry[ScheduleId, Schedule], o2: JMap.Entry[ScheduleId, Schedule]): Int =
        (for {
          time1 <- triggerTimeCache.get(o1.getKey)
          time2 <- triggerTimeCache.get(o2.getKey)
        } yield time1.compareTo(time2)).get

    }

    val paging = new PagingPredicate(
      cachingPredicate.asInstanceOf[Predicate[_, _]],
      cachingPredicate.asInstanceOf[Comparator[JMap.Entry[_, _]]],
      50
    )

    HazelcastTraversable(scheduleMap, paging)
  }

  override def getExecutions(f: Execution => Boolean): Traversable[Execution] = {
    val ordering: Ordering[(ExecutionId, Execution)] = Ordering.by(_._2.lastStatusChange)
    def filter(executionId: ExecutionId, execution: Execution): Boolean = f(execution)
    HazelcastTraversable(executionMap, ordering, 50, filter).map(_._2)
  }

  override def updateExecution[T](executionId: ExecutionId, stage: Execution.Stage)(f: Execution => T): T = {
    executionMap.lock(executionId)
    try {
      val execution = executionMap.get(executionId) << stage
      executionMap.put(executionId, execution)
      f(execution)
    } finally {
      executionMap.unlock(executionId)
    }
  }

}
