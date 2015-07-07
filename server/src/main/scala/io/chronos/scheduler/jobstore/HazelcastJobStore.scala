package io.chronos.scheduler.jobstore

import java.time.{Clock, ZonedDateTime}
import java.util.Map.Entry

import com.hazelcast.client.HazelcastClient
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.query.{PagingPredicate, Predicate}
import io.chronos.id._
import io.chronos.{JobDefinition, Work}

import scala.collection.JavaConversions._

/**
 * Created by domingueza on 07/07/15.
 */
object HazelcastJobStore {

  private class OverdueJobPredicate extends Predicate[JobId, JobDefinition] with Serializable {

    override def apply(entry: Entry[JobId, JobDefinition]): Boolean = {
      val hazelcastInstance = HazelcastClient.newHazelcastClient()
      val executionHistory = hazelcastInstance.getMap[JobId, ZonedDateTime]("executionHistory")

      val clock = Clock.systemUTC()
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

class HazelcastJobStore(private val hazelcastInstance: HazelcastInstance) extends JobQueue with WorkFactory {
  import HazelcastJobStore._

  private val jobDefinitions = hazelcastInstance.getMap[JobId, JobDefinition]("jobDefinitions")
  private val executionCounter = hazelcastInstance.getAtomicLong("executionCounter")

  override def pollOverdueJobs(clock: Clock, batchSize: Int)(implicit consumer: JobConsumer) = {
    val predicate = new PagingPredicate(new OverdueJobPredicate(), batchSize)
    jobDefinitions.values(predicate).foreach { jobDef =>
      jobDefinitions.remove(jobDef.jobId)
      consumer(jobDef)
    }
  }

  override def createWork(jobDef: JobDefinition): Work = {
    val workId: WorkId = (jobDef.jobId, executionCounter.incrementAndGet())
    Work(id = workId, params = jobDef.params, jobSpec = jobDef.jobSpec, jobDef.timeout)
  }

  override def push(jobDef: JobDefinition): Unit = {
    jobDefinitions.put(jobDef.jobId, jobDef)
  }

}
