package io.chronos.scheduler.store

import java.time.{Clock, ZonedDateTime}
import java.util.function.BiFunction
import java.util.{Collection => JCollection, Map => JMap}

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
class HazelcastStore(val hazelcastInstance: HazelcastInstance) extends ExecutionCache with HazelcastRegistryCache
  with HazelcastExecutionQueue with Logging {

  // Distributed data structures
  private val beating = hazelcastInstance.getAtomicReference[Boolean]("beating")
  beating.set(false)

  private val scheduleCounter = hazelcastInstance.getAtomicLong("scheduleCounter")
  private val scheduleMap = hazelcastInstance.getMap[ScheduleId, Schedule]("scheduleMap")

  private val executionCounter = hazelcastInstance.getAtomicLong("executionCounter")
  private val executionMap = hazelcastInstance.getMap[ExecutionId, Execution]("executions")
  private val executionsBySchedule = hazelcastInstance.getMap[ScheduleId, List[ExecutionId]]("executionsBySchedule")

  override def getSchedule(scheduleId: ScheduleId): Option[Schedule] =
    Option(scheduleMap.get(scheduleId))

  override def getScheduledJobs: Seq[(ScheduleId, Schedule)] = {
    val clock = Clock.systemUTC()
    val ordering: Ordering[(ScheduleId, Schedule)] = Ordering.by(p => nextExecutionTime(p._1, p._2).orNull)
    HazelcastTraversable(scheduleMap, ordering, 50).toStream
  }

  override def getExecution(executionId: ExecutionId): Option[Execution] =
    Option(executionMap.get(executionId))

  override def getExecutions(f: Execution => Boolean): Seq[Execution] = {
    val ordering: Ordering[(ExecutionId, Execution)] = Ordering.by(_._2.lastStatusChange)
    def filter(executionId: ExecutionId, execution: Execution): Boolean = f(execution)
    HazelcastTraversable(executionMap, ordering, 50, filter).map(_._2).toStream
  }

  override def schedule(jobSchedule: Schedule)(implicit clock: Clock): Execution = {
    val scheduleId = (jobSchedule.jobId, scheduleCounter.incrementAndGet())
    scheduleMap.put(scheduleId, jobSchedule)
    defineExecutionFor(scheduleId)
  }
  
  override def reschedule(scheduleId: ScheduleId)(implicit clock: Clock): Execution = defineExecutionFor(scheduleId)

  def currentExecutionOf(scheduleId: ScheduleId): Option[ExecutionId] =
    Option(executionsBySchedule.get(scheduleId)) flatMap { _.headOption }

  override def sweepOverdueExecutions(batchLimit: Int)(f: ExecutionId => Unit)(implicit clock: Clock): Unit =
    if (beating.compareAndSet(false, true)) {
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
      beating.set(false)
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
  
  private def collectFrom[T](iterator: Iterator[T], acc: Vector[T]): Vector[T] =
    if (iterator.isEmpty) acc
    else collectFrom(iterator, iterator.next() +: acc)
  
}
