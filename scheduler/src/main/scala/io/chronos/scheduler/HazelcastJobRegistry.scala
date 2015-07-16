package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}

import com.hazelcast.core.HazelcastInstance
import io.chronos._
import io.chronos.id.{ExecutionId, JobId, ScheduleId}

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 09/07/15.
 */
class HazelcastJobRegistry(val hazelcastInstance: HazelcastInstance) extends JobRepository with ExecutionPlan with ExecutionQueue {

  private val jobRegistry = hazelcastInstance.getMap[JobId, JobSpec]("jobRegistry")

  private val fetchLock = hazelcastInstance.getLock("fetchLock")

  private val scheduleCounter = hazelcastInstance.getAtomicLong("scheduleCounter")
  private val scheduleMap = hazelcastInstance.getMap[ScheduleId, JobSchedule]("scheduleMap")
  private val scheduleByJob = hazelcastInstance.getMap[JobId, ScheduleId]("scheduleByHJob")

  private val executionCounter = hazelcastInstance.getAtomicLong("executionCounter")
  private val executionMap = hazelcastInstance.getMap[ExecutionId, Execution]("executions")
  private val executionBySchedule = hazelcastInstance.getMap[ScheduleId, ExecutionId]("executionBySchedule")

  private val executionQueue = hazelcastInstance.getQueue[Execution]("executionQueue")

  override def availableSpecs: Seq[JobSpec] = {
    collectFrom(jobRegistry.values().iterator(), Vector())
  }

  override def publishSpec(jobSpec: JobSpec): Unit = jobRegistry.put(jobSpec.id, jobSpec)

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

  def scheduledJobs: Seq[(ScheduleId, JobSchedule)] =
    collectFrom[(ScheduleId, JobSchedule)](scheduleMap.iterator, Vector[(ScheduleId, JobSchedule)]())

  def executions(filter: Execution => Boolean): Seq[Execution] =
    collectFrom(executionMap.values().filter(filter).iterator, Vector())

  def hasPendingExecutions = executionQueue nonEmpty

  def nextExecution = executionQueue.take()

  def executionTimeout(executionId: ExecutionId): Option[FiniteDuration] =
    scheduleById(executionId._1).flatMap(job => job.timeout)

  def fetchOverdueExecutions(batchSize: Int)(consumer: Execution => Unit)(implicit clock: Clock): Unit = {
    var itemCount: Int = 0

    def notInProgress(pair: (ScheduleId, JobSchedule)): Boolean = Option(executionBySchedule.get(pair._1)).
      map(executionId => executionMap.get(executionId)).
      map(execution => execution.stage) match {
      case Some(_: Execution.InProgress) => false
      case _                             => true
    }

    def underBatchLimit(pair: (ScheduleId, JobSchedule)): Boolean = itemCount < batchSize

    fetchLock.lockInterruptibly()
    try {
      for ((scheduleId, schedule) <- scheduleMap.toMap.filter(notInProgress).takeWhile(underBatchLimit)) {
        referenceExecutionTime(scheduleId) match {
          case Some(either) =>
            val nextExecutionTime = schedule.trigger.nextExecutionTime(clock, either)
            val now = ZonedDateTime.now(clock)

            nextExecutionTime match {
              case Some(time) if time.isBefore(now) || time.isEqual(now) =>
                val execution = executionById(executionBySchedule.get(scheduleId)).get
                itemCount += 1
                consumer(execution)
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
  
  def updateExecution(executionId: ExecutionId, newStage: Execution.Stage)(consumer: Execution => Unit): Unit = {
    require(executionMap.containsKey(executionId), s"There is no execution with ID $executionId")
    require(scheduleMap.containsKey(executionId._1), s"There is no schedule with ID ${executionId._1}")

    executionMap.lock(executionId)
    try {
      val updated = executionById(executionId) map (_ << newStage) get;
      newStage match {
        case Execution.Triggered(_) =>
          executionQueue.put(updated)

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
    if (!iterator.hasNext) {
      accumulator
    } else {
      collectFrom(iterator, accumulator :+ iterator.next())
    }
  }

}
