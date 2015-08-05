package io.chronos.scheduler.internal.cache

import java.time.{Clock, ZonedDateTime}
import java.util.function.BiFunction
import java.util.{Map => JMap}

import com.hazelcast.core.HazelcastInstance
import io.chronos.Trigger.{LastExecutionTime, ReferenceTime, ScheduledTime}
import io.chronos.id._
import io.chronos.scheduler.ExecutionCache
import io.chronos.{Execution, Schedule}
import org.slf4s.Logging

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 01/08/15.
 */
class HazelcastStore(val hazelcastInstance: HazelcastInstance) extends HazelcastExecutionCache
  with HazelcastRegistryCache with ExecutionCache
  with HazelcastExecutionQueue with Logging {

  // Distributed data structures
  private val sweeping = hazelcastInstance.getAtomicReference[Boolean]("sweeping")
  sweeping.set(false)

  override def getScheduledJobs: Seq[(ScheduleId, Schedule)] = {
    val clock = Clock.systemUTC()
    val ordering: Ordering[(ScheduleId, Schedule)] = Ordering.by(p => nextExecutionTime(p._1, p._2).orNull)
    HazelcastTraversable(scheduleMap, ordering, 50).toStream
  }

  override def schedule(jobSchedule: Schedule)(implicit clock: Clock): Execution = {
    val scheduleId = (jobSchedule.jobId, scheduleCounter.incrementAndGet())
    scheduleMap.put(scheduleId, jobSchedule)
    defineExecutionFor(scheduleId)
  }
  
  override def reschedule(scheduleId: ScheduleId)(implicit clock: Clock): Execution = defineExecutionFor(scheduleId)

  override def sweepOverdueExecutions(batchLimit: Int)(f: ExecutionId => Unit)(implicit clock: Clock): Unit =
    if (sweeping.compareAndSet(false, true)) {
      var itemCount = 0
      def underBatchLimit: Boolean = itemCount < batchLimit

      def ready(scheduleId: ScheduleId): Boolean = (for {
        schedule <- Option(scheduleMap.get(scheduleId))
        execution <- currentExecutionOf(scheduleId).map(executionMap.get)
      } yield execution is Execution.Ready).getOrElse(false)

      def schedules: Iterable[(ScheduleId, Schedule)] =
        scheduleMap.entrySet().view.
          filter(entry => ready(entry.getKey)).
          takeWhile(_ => underBatchLimit).
          map(entry => (entry.getKey, entry.getValue))

      val now = ZonedDateTime.now(clock)
      for {
        (scheduleId, schedule) <- schedules
        nextTime <- nextExecutionTime(scheduleId, schedule) if nextTime.isBefore(now) || nextTime.isEqual(now)
        exec     <- currentExecutionOf(scheduleId)
      } {
        itemCount += 1  // This awful statement is there to help the upper helper functions to build the batch
        f(exec)
      }
      
      // Reset the atomic boolean flag to allow for more "sweeps"
      sweeping.set(false)
    }

  def nextExecutionTime(scheduleId: ScheduleId)(implicit clock: Clock): Option[ZonedDateTime] =
    Option(scheduleMap.get(scheduleId)).flatMap(s => nextExecutionTime(scheduleId, s))

  private def nextExecutionTime(scheduleId: ScheduleId, schedule: Schedule)(implicit clock: Clock): Option[ZonedDateTime] =
    referenceTime(scheduleId).flatMap(time => schedule.trigger.nextExecutionTime(time))

  private def referenceTime(scheduleId: ScheduleId): Option[ReferenceTime] = {
    import Execution._

    def executionAt[T <: Stage](scheduleId: ScheduleId)(implicit stage: StageLike[T]): Option[Execution] =
      Option(executionsBySchedule.get(scheduleId)) flatMap { execIds =>
        execIds.find { execId =>
          Option(executionMap.get(execId)).exists(stage.currentIn)
        }
      } map executionMap.get

    executionAt[Finished](scheduleId).map { exec =>
      LastExecutionTime(exec.stage.when)
    } orElse executionAt[Scheduled](scheduleId).map { exec =>
      ScheduledTime(exec.stage.when)
    }
  }

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
