package io.chronos.scheduler.worker

import akka.actor.{Actor, ActorLogging, Props}
import com.hazelcast.client.HazelcastClient
import io.chronos.scheduler.JobDefinition
import io.chronos.scheduler.id.{JobId, WorkId}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class ExecuteWork(workId: WorkId)
  case class CompletedWork(workId: WorkId, result: Any)
  
  def props = Props[JobExecutor]
}

class JobExecutor extends Actor with ActorLogging {
  import JobExecutor._

  private val hazelcastClient = HazelcastClient.newHazelcastClient()
  private val jobDefinitions = hazelcastClient.getMap[JobId, JobDefinition]("jobDefinitions")

  def receive = {
    case ExecuteWork(workId) =>
      log.info("Executing work {}", workId)
      val jobDef = jobDefinitions.get(workId._1)
      val jobInstance = jobDef.job.newInstance()
      
      jobDef.job.getDeclaredFields.
        filter(field => jobDef.params.contains(field.getName)).
        foreach { field =>
          val paramValue = jobDef.params(field.getName)
          field.set(jobInstance, paramValue)
        }
      
      val result = jobInstance.execute()
      sender() ! CompletedWork(workId, result)
  }

}
