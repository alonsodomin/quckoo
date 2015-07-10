package io.chronos.scheduler.runtime

import java.time.{Clock, ZonedDateTime}

import com.hazelcast.core.HazelcastInstance
import io.chronos.id.{ExecutionId, JobId, ScheduleId}
import io.chronos.scheduler.JobRepository
import io.chronos.{JobSchedule, JobSpec}

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 09/07/15.
 */
class JobRegistry(val clock: Clock, val hazelcastInstance: HazelcastInstance) extends JobRepository {

  private val jobRegistry = hazelcastInstance.getMap[JobId, JobSpec]("jobRegistry")

  private val scheduleCounter = hazelcastInstance.getAtomicLong("scheduleCounter")
  private val scheduleMap = hazelcastInstance.getMap[ScheduleId, JobSchedule]("scheduleMap")
  private val scheduleByJob = hazelcastInstance.getMap[JobId, ScheduleId]("scheduleByHJob")

  private val executionCounter = hazelcastInstance.getAtomicLong("executionCounter")
  private val executionMap = hazelcastInstance.getMap[ExecutionId, Execution]("executions")
  private val executionBySchedule = hazelcastInstance.getMap[ScheduleId, ExecutionId]("executionBySchedule")

  private val executionQueue = hazelcastInstance.getQueue[Execution]("executionQueue")

  override def availableSpecs: Seq[JobSpec] = jobRegistry.values().toSeq

  override def publishSpec(jobSpec: JobSpec): Unit = {
    jobRegistry.put(jobSpec.id, jobSpec)
  }

  def apply(scheduleId: ScheduleId): Option[JobSchedule] = Option(scheduleMap.get(scheduleId))

  def apply(executionId: ExecutionId): Option[Execution] = Option(executionMap.get(executionId))

  def schedule(schedule: JobSchedule): ScheduleId = {
    require(jobRegistry.containsKey(schedule.jobId), s"The job specification ${schedule.jobId} has not been registered yet.")

    val scheduleId = (schedule.jobId, scheduleCounter.incrementAndGet())
    scheduleMap.put(scheduleId, schedule)
    scheduleByJob.put(schedule.jobId, scheduleId)

    scheduleId
  }

  def scheduledJobs: Seq[(ScheduleId, JobSchedule)] = {
    var allSchedules: List[(ScheduleId, JobSchedule)] = Nil
    for ((scheduleId, schedule) <- scheduleMap.toMap) {
      allSchedules = (scheduleId, schedule) :: allSchedules
    }
    allSchedules
  }

  def hasPendingExecutions = executionQueue nonEmpty

  def nextExecution = executionQueue.take()

  def pollOverdueJobs(batchSize: Int)(implicit c: Execution => Unit): Unit = {
    var itemCount: Int = 0
    for ((scheduleId, schedule) <- scheduleMap.toMap.takeWhile(_ => itemCount < batchSize)) {
      val lastExecutionTime = lastExecutionTime(scheduleId)
      val nextExecutionTime = schedule.trigger.nextExecutionTime(clock, lastExecutionTime)
      val now = ZonedDateTime.now(clock)

      nextExecutionTime match {
        case Some(time) if time.isBefore(now) || time.isEqual(now) =>
          val execution = createExecution(scheduleId, schedule)
          itemCount += 1
          c(execution)
        case Some(_) =>
        case None =>
      }
    }
  }
  
  def update(executionId: ExecutionId, status: Execution.Status): Unit = {
    require(executionMap.containsKey(executionId), s"There is no execution with ID $executionId")
    require(scheduleMap.containsKey(executionId._1), s"There is no schedule with ID ${executionId._1}")

    executionMap.lock(executionId)
    try {
      val updated = this(executionId) map (e => e.update(status)) get;
      status match {
        case Execution.Finished(_, _, _) =>
          scheduleMap.lock(executionId._1)
          try {
            if (!scheduleMap.get(executionId._1).isRecurring) {
              scheduleMap.remove(executionId._1)
            }
          } finally {
            scheduleMap.unlock(executionId._1)
          }
        case _ =>
      }
      executionMap.put(executionId, updated)
    } finally {
      executionMap.unlock(executionId)
    }
  }

  def lastExecutionTime(scheduleId: ScheduleId): Option[ZonedDateTime] = {
    Option(executionBySchedule.get(scheduleId)).
      flatMap (execId => Option(executionMap.get(execId))).
      map (exec => exec.status) match {
      case Some(Execution.Finished(when, _)) => Some(when)
      case None => None
    }
  }

  private def createExecution(scheduleId: ScheduleId, schedule: JobSchedule): Execution = {
    val executionId = (scheduleId, executionCounter.incrementAndGet())
    val execution = Execution.scheduled(executionId, ZonedDateTime.now(clock))
    executionMap.put(executionId, execution)
    executionBySchedule.put(scheduleId, executionId)

    execution
  }
  
}
