package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}

import com.hazelcast.core.HazelcastInstance
import io.chronos._
import io.chronos.id.{ExecutionId, JobId, ScheduleId}

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.concurrent.duration.FiniteDuration
import scala.util.{Success, Try}

/**
 * Created by aalonsodominguez on 09/07/15.
 */
class HazelcastJobRegistry(val hazelcastInstance: HazelcastInstance) {

  private val jobRegistry = hazelcastInstance.getMap[JobId, JobSpec]("jobRegistry")

  private val fetchLock = hazelcastInstance.getLock("fetchLock")

  private val scheduleCounter = hazelcastInstance.getAtomicLong("scheduleCounter")
  private val scheduleMap = hazelcastInstance.getMap[ScheduleId, JobSchedule]("scheduleMap")
  private val scheduleByJob = hazelcastInstance.getMap[JobId, ScheduleId]("scheduleByHJob")

  private val executionCounter = hazelcastInstance.getAtomicLong("executionCounter")
  private val executionMap = hazelcastInstance.getMap[ExecutionId, Execution]("executions")
  private val executionBySchedule = hazelcastInstance.getMap[ScheduleId, ExecutionId]("executionBySchedule")

  private val executionQueue = hazelcastInstance.getQueue[Execution]("executionQueue")

  def availableJobSpecs: Seq[JobSpec] = {
    collectFrom(jobRegistry.values().iterator(), Vector())
  }

  def registerJobSpec(jobSpec: JobSpec): Try[JobId] = {
    jobRegistry.put(jobSpec.id, jobSpec)
    Success(jobSpec.id)
  }

  def specById(jobId: JobId): Option[JobSpec] = Option(jobRegistry.get(jobId))

  def scheduleById(scheduleId: ScheduleId): Option[JobSchedule] = Option(scheduleMap.get(scheduleId))

  def executionById(executionId: ExecutionId): Option[Execution] = Option(executionMap.get(executionId))

  def specOf(executionId: ExecutionId): Option[JobSpec] = specById(executionId._1._1)

  def scheduleOf(executionId: ExecutionId): Option[JobSchedule] = scheduleById(executionId._1)

  def schedule(schedule: JobSchedule)(implicit clock: Clock): Execution = {
    require(jobRegistry.containsKey(schedule.jobId), s"The job specification ${schedule.jobId} has not been registered yet.")

    val scheduleId = (schedule.jobId, scheduleCounter.incrementAndGet())
    scheduleMap.put(scheduleId, schedule)
    scheduleByJob.put(schedule.jobId, scheduleId)

    createExecution(scheduleId, schedule)
  }

  def reschedule(scheduleId: ScheduleId)(implicit clock: Clock): Execution = {
    require(scheduleMap.containsKey(scheduleId), s"There is not any job schedule for id: $scheduleId}")
    createExecution(scheduleId, scheduleMap.get(scheduleId))
  }

  def scheduledJobs: Seq[(ScheduleId, JobSchedule)] =
    collectFrom[(ScheduleId, JobSchedule)](scheduleMap.iterator, Vector[(ScheduleId, JobSchedule)]())

  def executions(filter: Execution => Boolean): Seq[Execution] =
    collectFrom(executionMap.values().filter(filter).iterator, Vector())

  def hasPendingExecutions = executionQueue nonEmpty

  def nextExecution = executionQueue.take()

  def executionTimeout(executionId: ExecutionId): Option[FiniteDuration] =
    scheduleById(executionId._1).flatMap(_.timeout)

  def fetchOverdueExecutions(batchSize: Int)(consumer: Execution => Unit)(implicit clock: Clock): Unit = {
    var itemCount: Int = 0

    def notInProgress(pair: (ScheduleId, JobSchedule)): Boolean = Option(executionBySchedule.get(pair._1)).
      map(executionMap.get).
      map(_.stage) match {
      case Some(_: Execution.InProgress) => false
      case _                             => true
    }

    def underBatchLimit(pair: (ScheduleId, JobSchedule)): Boolean = itemCount < batchSize

    if(fetchLock.tryLock()) {
      try {
        for ((scheduleId, schedule) <- scheduleMap.toMap.view filter notInProgress takeWhile underBatchLimit) {
          referenceExecutionTime(scheduleId) match {
            case Some(scheduledOrLastExecutedTime) =>
              val nextExecutionTime = schedule.trigger.nextExecutionTime(clock, scheduledOrLastExecutedTime)
              val now = ZonedDateTime.now(clock)

              nextExecutionTime match {
                case Some(time) if time.isBefore(now) || time.isEqual(now) =>
                  executionById(executionBySchedule.get(scheduleId)).foreach { execution =>
                    itemCount += 1
                    consumer(execution)
                  }
                case Some(_) =>
                case None =>
              }

            case _ =>
          }
        }
      } finally {
        fetchLock.unlock()
      }
    }
  }
  
  def updateExecution(executionId: ExecutionId, newStage: Execution.Stage)(consumer: Execution => Unit): Unit = {
    require(executionMap.containsKey(executionId), s"There is no execution with ID $executionId")
    require(scheduleMap.containsKey(executionId._1), s"There is no schedule with ID ${executionId._1}")

    executionMap.lock(executionId)
    try {
      val updated = executionById(executionId) map (_ << newStage) get;
      newStage match {
        case _ : Execution.Triggered =>
          executionQueue.put(updated)

        case _ : Execution.Finished =>
          val (scheduleId, _) = executionId
          scheduleMap.lock(scheduleId)
          try {
            val schedule = scheduleMap.get(scheduleId)
            if (!schedule.isRecurring) {
              scheduleMap.remove(scheduleId)
            }
            executionBySchedule.remove(scheduleId)
          } finally {
            scheduleMap.unlock(scheduleId)
          }
          
        case _ =>
      }

      executionMap.put(executionId, updated)
      consumer(updated)
    } finally {
      executionMap.unlock(executionId)
    }
  }

  def referenceExecutionTime(scheduleId: ScheduleId): Option[Either[ZonedDateTime, ZonedDateTime]] = {
    Option(executionBySchedule.get(scheduleId)).
      flatMap (execId => Option(executionMap.get(execId))).
      map (exec => exec.stage) match {
      case Some(Execution.Finished(when, _, _)) => Some(Right(when))
      case Some(Execution.Scheduled(when))      => Some(Left(when))
      case _                                    => None
    }
  }

  def lastExecutionTime(scheduleId: ScheduleId): Option[ZonedDateTime] = {
    Option(executionBySchedule.get(scheduleId)).
      flatMap (execId => Option(executionMap.get(execId))).
      map (exec => exec.stage) match {
      case Some(Execution.Finished(when, _, _)) => Some(when)
      case _                                    => None
    }
  }

  private def createExecution(scheduleId: ScheduleId, schedule: JobSchedule)(implicit clock: Clock): Execution = {
    val executionId = (scheduleId, executionCounter.incrementAndGet())
    val execution = Execution(executionId)
    executionMap.put(executionId, execution)
    executionBySchedule.put(scheduleId, executionId)
    execution
  }

  @tailrec
  private def collectFrom[T](iterator: Iterator[T], accumulator: Seq[T]): Seq[T] = {
    if (!iterator.hasNext) accumulator
    else collectFrom(iterator, accumulator :+ iterator.next())
  }

}
