package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}
import java.util.function.BiFunction

import com.hazelcast.core.Hazelcast
import io.chronos.id.{ExecutionId, JobId, ScheduleId}
import io.chronos.{JobDefinition, JobSchedule, JobSpec}

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 09/07/15.
 */
object JobRegistry {


}

class JobRegistry(clock: Clock) {

  val hazelcastInstance = Hazelcast.newHazelcastInstance()

  val jobRegistry = hazelcastInstance.getMap[JobId, JobSpec]("jobRegistry")
  
  val scheduleCounter = hazelcastInstance.getAtomicLong("scheduleCounter")
  val scheduleMap = hazelcastInstance.getMap[ScheduleId, JobSchedule]("scheduleMap")
  val scheduleByJob = hazelcastInstance.getMap[JobId, ScheduleId]("scheduleByHJob")
  
  val executionCounter = hazelcastInstance.getAtomicLong("executionCounter")
  val executionMap = hazelcastInstance.getMap[ExecutionId, Execution]("executions")
  val executionBySchedule = hazelcastInstance.getMap[ScheduleId, ExecutionId]("executionBySchedule")

  val pendingExecutions = hazelcastInstance.getQueue[Execution]("pendingExec")

  def register(jobSpec: JobSpec): Unit = {
    jobRegistry.put(jobSpec.id, jobSpec)
  }

  def schedule(jobDef: JobDefinition): ScheduleId = {
    require(jobRegistry.containsKey(jobDef.jobId), s"The job specification ${jobDef.jobId} has not been registered yet.")

    val scheduleId = (jobDef.jobId, scheduleCounter.incrementAndGet())
    scheduleMap.put(scheduleId, JobSchedule(scheduleId, jobDef))
    scheduleByJob.put(jobDef.jobId, scheduleId)

    scheduleId
  }

  def pollOverdueJobs(batchSize: Int)(implicit c: Execution => Unit): Unit = {
    var itemCount: Int = 0
    for (schedule <- scheduleMap.values().toSeq.takeWhile(_ => itemCount < batchSize)) {
      val lastExecutionTime = lastExecutionTime(schedule.id)
      val nextExecutionTime = schedule.jobDef.trigger.nextExecutionTime(clock, lastExecutionTime)
      val now = ZonedDateTime.now(clock)

      nextExecutionTime match {
        case Some(time) if time.isBefore(now) || time.isEqual(now) =>
          val execution = createExecution(schedule)
          itemCount += 1
          c(execution)
        case Some(_) =>
        case None =>
      }
    }
  }
  
  def update(executionId: ExecutionId, status: Execution.Status): Unit = {
    val remappingFunc = new BiFunction[ExecutionId, Execution, Execution] {
      override def apply(id: ExecutionId, old: Execution): Execution = {
        old.update(status)
      }
    }

    status match {
      case Execution.Triggered(_) =>
    }

    executionMap.computeIfPresent(executionId, remappingFunc)
  }

  def isWaiting(scheduleId: ScheduleId): Boolean = Option(executionBySchedule.get(scheduleId)) match {
    case Some(executionId) => Option(executionMap.get(executionId)) match {
      case Some(execution) => execution.isWaiting
      case None            => false
    }
    case None => false
  }

  def lastExecutionTime(scheduleId: ScheduleId): Option[ZonedDateTime] = {
    val status = Option(executionBySchedule.get(scheduleId)).
      flatMap (execId => Option(executionMap.get(execId))).
      map (exec => exec.status)
    status match {
      case Some(Execution.Finished(when, _)) => Some(when)
      case None => None
    }
  }

  private def createExecution(schedule: JobSchedule): Execution = {
    val executionId = (schedule.id, executionCounter.incrementAndGet())
    val execution = Execution.scheduled(executionId, ZonedDateTime.now(clock))
    executionMap.put(executionId, execution)
    executionBySchedule.put(schedule.id, executionId)

    execution
  }
  
}
