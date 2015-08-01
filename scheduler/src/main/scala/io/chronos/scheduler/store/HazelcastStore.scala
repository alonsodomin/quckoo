package io.chronos.scheduler.store

import java.time.ZonedDateTime

import com.hazelcast.core.HazelcastInstance
import io.chronos.Execution.StageLike
import io.chronos.Trigger.{LastExecutionTime, ReferenceTime, ScheduledTime}
import io.chronos.id._
import io.chronos.scheduler.{ExecutionPlan, JobRegistry}
import io.chronos.{Execution, JobSchedule, JobSpec}

import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
 * Created by aalonsodominguez on 01/08/15.
 */
class HazelcastStore(hazelcastInstance: HazelcastInstance) extends ExecutionPlan with JobRegistry {

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

  def getSchedule(scheduleId: ScheduleId): Option[JobSchedule] =
    Option(scheduleMap.get(scheduleId))

  override def getJob(jobId: JobId): Option[JobSpec] = Option(jobSpecCache.get(jobId))

  override def registerJob(jobSpec: JobSpec): Unit = jobSpecCache.put(jobSpec.id, jobSpec)

  override def getJobs: Seq[JobSpec] = collectFrom(jobSpecCache.values().iterator(), Vector())

  def currentExecutionOf(scheduleId: ScheduleId): Option[Execution] =
    Option(executionsBySchedule.get(scheduleId)) flatMap { _.headOption } map executionMap.get

  def updateExecution[T](executionId: ExecutionId, stage: Execution.Stage)(f: Execution => T): Future[T] = Future {
    executionMap.lock(executionId)
    try {
      val execution = executionMap.get(executionId) << stage
      executionMap.put(executionId, execution)
      f(execution)
    } finally {
      executionMap.unlock(executionId)
    }
  }

  def nextExecutionTime(scheduleId: ScheduleId, schedule: JobSchedule): Option[ZonedDateTime] =
    (for (time <- referenceTime(scheduleId)) yield schedule.trigger.nextExecutionTime(time)).flatten

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

  private def collectFrom[T](iterator: Iterator[T], acc: Vector[T]): Vector[T] =
    if (iterator.isEmpty) acc
    else collectFrom(iterator, iterator.next() +: acc)
}
