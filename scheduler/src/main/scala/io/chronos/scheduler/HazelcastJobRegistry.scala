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

  def getSpec(jobId: JobId): Option[JobSpec] = Option(jobRegistry.get(jobId))

  def getSchedule(scheduleId: ScheduleId): Option[JobSchedule] = Option(scheduleMap.get(scheduleId))

  def getExecution(executionId: ExecutionId): Option[Execution] = Option(executionMap.get(executionId))

  def specOf(executionId: ExecutionId): JobSpec = getSpec(executionId._1._1).get

  def scheduleOf(executionId: ExecutionId): JobSchedule = getSchedule(executionId._1).get

  def schedule(clock: Clock, schedule: JobSchedule): ExecutionId = {
    require(jobRegistry.containsKey(schedule.jobId), s"The job specification ${schedule.jobId} has not been registered yet.")

    val scheduleId = (schedule.jobId, scheduleCounter.incrementAndGet())
    scheduleMap.put(scheduleId, schedule)
    scheduleByJob.put(schedule.jobId, scheduleId)

    createExecution(clock, scheduleId, schedule).executionId
  }

  def scheduledJobs: Seq[(ScheduleId, JobSchedule)] =
    collectFrom[(ScheduleId, JobSchedule)](scheduleMap.iterator, Vector[(ScheduleId, JobSchedule)]())

  def executions: Seq[Execution] = collectFrom(executionMap.values().iterator(), Vector())

  def hasPendingExecutions = executionQueue nonEmpty

  def nextExecution = executionQueue.take()

  def executionTimeout(executionId: ExecutionId): Option[FiniteDuration] =
    getSchedule(executionId._1).flatMap(job => job.executionTimeout)

  def fetchOverdueExecutions(clock: Clock, batchSize: Int)(c: Execution => Unit): Unit = {
    var itemCount: Int = 0

    def notInProgress(pair: (ScheduleId, JobSchedule)): Boolean = Option(executionBySchedule.get(pair._1)).
      map(executionId => executionMap.get(executionId)).
      map(execution => execution.status) match {
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
                val execution = getExecution(executionBySchedule.get(scheduleId)).get
                itemCount += 1
                c(execution)
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
  
  def updateExecution(executionId: ExecutionId, status: Execution.Status)(c: Execution => Unit): Unit = {
    require(executionMap.containsKey(executionId), s"There is no execution with ID $executionId")
    require(scheduleMap.containsKey(executionId._1), s"There is no schedule with ID ${executionId._1}")

    executionMap.lock(executionId)
    try {
      val updated = getExecution(executionId) map (e => e.copy(statusHistory = status :: e.statusHistory)) get;
      status match {
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
      c(updated)
    } finally {
      executionMap.unlock(executionId)
    }
  }

  def referenceExecutionTime(scheduleId: ScheduleId): Option[Either[ZonedDateTime, ZonedDateTime]] = {
    Option(executionBySchedule.get(scheduleId)).
      flatMap (execId => Option(executionMap.get(execId))).
      map (exec => exec.status) match {
      case Some(Execution.Finished(when, _, _)) => Some(Right(when))
      case Some(Execution.Scheduled(when))      => Some(Left(when))
      case _ => None
    }
  }

  def lastExecutionTime(scheduleId: ScheduleId): Option[ZonedDateTime] = {
    Option(executionBySchedule.get(scheduleId)).
      flatMap (execId => Option(executionMap.get(execId))).
      map (exec => exec.status) match {
      case Some(Execution.Finished(when, _, _)) => Some(when)
      case _ => None
    }
  }

  private def createExecution(clock: Clock, scheduleId: ScheduleId, schedule: JobSchedule): Execution = {
    val executionId = (scheduleId, executionCounter.incrementAndGet())
    val execution = Execution(executionId, Execution.Scheduled(ZonedDateTime.now(clock)) :: Nil)
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
