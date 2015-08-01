package io.chronos.scheduler.store

import java.time.{Clock, ZonedDateTime}
import java.util.function.BiFunction

import com.hazelcast.core.HazelcastInstance
import io.chronos.Execution.StageLike
import io.chronos.Trigger.{LastExecutionTime, ReferenceTime, ScheduledTime}
import io.chronos.id._
import io.chronos.scheduler.{ExecutionPlan, JobRegistry}
import io.chronos.{Execution, JobSchedule, JobSpec}
import org.slf4s.Logging

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 01/08/15.
 */
class HazelcastStore(hazelcastInstance: HazelcastInstance) extends ExecutionPlan with JobRegistry with Logging {

  // Distributed data structures
  private val jobSpecCache = hazelcastInstance.getMap[JobId, JobSpec]("jobSpecCache")

  private val beating = hazelcastInstance.getAtomicReference[Boolean]("beating")
  beating.set(false)

  private val scheduleCounter = hazelcastInstance.getAtomicLong("scheduleCounter")
  private val scheduleMap = hazelcastInstance.getMap[ScheduleId, JobSchedule]("scheduleMap")

  private val executionCounter = hazelcastInstance.getAtomicLong("executionCounter")
  private val executionMap = hazelcastInstance.getMap[ExecutionId, Execution]("executions")
  private val executionsBySchedule = hazelcastInstance.getMap[ScheduleId, List[ExecutionId]]("executionsBySchedule")

  private val executionQueue = hazelcastInstance.getQueue[ExecutionId]("executionQueue")

  override def getJob(jobId: JobId): Option[JobSpec] = Option(jobSpecCache.get(jobId))

  override def registerJob(jobSpec: JobSpec): Unit = jobSpecCache.put(jobSpec.id, jobSpec)

  override def getJobs: Seq[JobSpec] = collectFrom(jobSpecCache.values().iterator(), Vector())

  override def getSchedule(scheduleId: ScheduleId): Option[JobSchedule] =
    Option(scheduleMap.get(scheduleId))

  override def getScheduledJobs: Seq[(ScheduleId, JobSchedule)] =
    collectFrom(scheduleMap.entrySet().map(entry => (entry.getKey, entry.getValue)).iterator, Vector())

  override def getExecution(executionId: ExecutionId): Option[Execution] =
    Option(executionMap.get(executionId))

  override def getExecutions(f: Execution => Boolean): Seq[Execution] =
    collectFrom(executionMap.values().filter(f).iterator, Vector())

  override def schedule(jobSchedule: JobSchedule): Execution = {
    val scheduleId = (jobSchedule.jobId, scheduleCounter.incrementAndGet())
    scheduleMap.put(scheduleId, jobSchedule)
    defineExecutionFor(scheduleId)
  }
  
  override def reschedule(scheduleId: ScheduleId): Execution = defineExecutionFor(scheduleId)

  override def hasPendingExecutions: Boolean = !executionQueue.isEmpty

  override def takePending(f: (ExecutionId, JobSchedule, JobSpec) => Unit): Unit = if (!executionQueue.isEmpty) {
    val executionId = executionQueue.take()
    for {
      jobSpec <- Option(jobSpecCache.get(executionId._1._1))
      jobSchedule <- Option(scheduleMap.get(executionId._1))
    } f(executionId, jobSchedule, jobSpec)
  }

  override def sweepOverdueExecutions(batchLimit: Int)(f: ExecutionId => Unit)(implicit clock: Clock): Unit =
    if (beating.compareAndSet(false, true)) {
      def currentExecutionOf(scheduleId: ScheduleId): Option[ExecutionId] =
        Option(executionsBySchedule.get(scheduleId)) flatMap { _.headOption }

      var itemCount = 0
      def underBatchLimit: Boolean = itemCount < batchLimit

      def ready(scheduleId: ScheduleId): Boolean = (for {
        schedule <- Option(scheduleMap.get(scheduleId))
        execution <- currentExecutionOf(scheduleId).map(executionMap.get)
      } yield execution is Execution.Ready).getOrElse(false)

      def schedules: Iterable[(ScheduleId, JobSchedule)] =
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

      stage match {
        case _: Execution.Triggered =>
          executionQueue.offer(executionId)
      }

      f(execution)
    } finally {
      executionMap.unlock(executionId)
    }
  }

  def nextExecutionTime(scheduleId: ScheduleId): Option[ZonedDateTime] =
    Option(scheduleMap.get(scheduleId)).flatMap(s => nextExecutionTime(scheduleId, s))

  private def nextExecutionTime(scheduleId: ScheduleId, schedule: JobSchedule): Option[ZonedDateTime] =
    referenceTime(scheduleId).flatMap(time => schedule.trigger.nextExecutionTime(time))

  private def referenceTime(scheduleId: ScheduleId): Option[ReferenceTime] = {
    def executionAt(scheduleId: ScheduleId, stage: StageLike[_]): Option[Execution] =
      Option(executionsBySchedule.get(scheduleId)) flatMap { execIds =>
        execIds.find { execId =>
          Option(executionMap.get(execId)).exists(stage.currentIn)
        }
      } map executionMap.get

    executionAt(scheduleId, Execution.Done).map { exec =>
      LastExecutionTime(exec.stage.when)
    } orElse executionAt(scheduleId, Execution.Ready).map { exec =>
      ScheduledTime(exec.stage.when)
    }
  }

  private def defineExecutionFor(scheduleId: ScheduleId): Execution = {
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
