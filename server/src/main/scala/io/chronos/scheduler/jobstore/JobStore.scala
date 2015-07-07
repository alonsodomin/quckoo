package io.chronos.scheduler.jobstore

import java.time.{Clock, ZonedDateTime}
import java.util.concurrent.atomic.AtomicLong

import io.chronos.scheduler.id.JobId
import io.chronos.scheduler.{JobDefinition, Work}

import scala.collection.mutable

/**
 * Created by aalonsodominguez on 07/07/15.
 */
class JobStore extends JobQueue with WorkFactory {
  private var jobQueue: List[JobDefinition] = Nil
  private val executionHistory: mutable.Map[JobId, ZonedDateTime] = mutable.Map.empty
  private val executionCounter = new AtomicLong(0)

  override def pollOverdueJobs(clock: Clock, batchSize: Int)(implicit consumer: JobConsumer): Unit = {
    var itemCount: Int = 0
    for ((jobDef, idx) <- jobQueue.view.zipWithIndex.takeWhile(_ => itemCount < batchSize)) {
      val lastExecutionTime = executionHistory.get(jobDef.jobId)
      val nextExecutionTime = jobDef.trigger.nextExecutionTime(clock, lastExecutionTime)
      val now = ZonedDateTime.now(clock)

      nextExecutionTime match {
        case Some(time) if time.isBefore(now) || time.isEqual(now) =>
          jobQueue = jobQueue.patch(idx, Nil, 1)
          consumer.apply(jobDef)
          itemCount = itemCount + 1
        case Some(_) =>
        case None =>
          jobQueue = jobQueue.patch(idx, Nil, 1)
      }
    }
  }

  override def push(jobDefinition: JobDefinition): Unit = {
    jobQueue = jobDefinition :: jobQueue
  }

  override def createWork(jobDef: JobDefinition): Work = {
    val workId = (jobDef.jobId, executionCounter.incrementAndGet())
    Work(workId, jobDef.params, jobDef.jobSpec, jobDef.timeout)
  }

}
