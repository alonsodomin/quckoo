package io.chronos.scheduler.jobstore

import java.time.{Clock, ZonedDateTime}
import java.util.Map.Entry

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.query.{PagingPredicate, Predicate}
import io.chronos.scheduler.JobDefinition
import io.chronos.scheduler.id._
import io.chronos.scheduler.worker.Work

import scala.collection.JavaConversions._

/**
 * Created by domingueza on 07/07/15.
 */
class HazelcastJobStore(private val hazelcastInstance: HazelcastInstance) extends ScheduledJobQueue with WorkFactory {

  private val jobDefinitions = hazelcastInstance.getMap[JobId, JobDefinition]("jobDefinitions")
  private val executionHistory = hazelcastInstance.getMap[JobId, ZonedDateTime]("executionHistory")
  private val executionCounter = hazelcastInstance.getAtomicLong("executionCounter")

  override def pollOverdueJobs(clock: Clock, batchSize: Int)(implicit consumer: JobConsumer) = {
    val predicate = new PagingPredicate(new OverdueJobPredicate(clock), batchSize)
    jobDefinitions.values(predicate).foreach { jobDef =>
      jobDefinitions.remove(jobDef.jobId)
      consumer(jobDef)
    }
  }

  override def createWork(jobDef: JobDefinition): Work = {
    val workId: WorkId = (jobDef.jobId, executionCounter.incrementAndGet())
    Work(workId)
  }

  override def push(jobDef: JobDefinition): Unit = {
    jobDefinitions.put(jobDef.jobId, jobDef)
  }

  private class OverdueJobPredicate(clock: Clock) extends Predicate[JobId, JobDefinition] {

    override def apply(entry: Entry[JobId, JobDefinition]): Boolean = {
      val now = ZonedDateTime.now(clock)

      val lastExecution = Option(executionHistory.get(entry.getKey))
      val nextExecution = entry.getValue.trigger.nextExecutionTime(clock, lastExecution)

      nextExecution match {
        case Some(dateTime) =>
          dateTime.isBefore(now) || dateTime.isEqual(now)
        case None =>
          false
      }
    }

  }

}
