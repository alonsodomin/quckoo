package io.chronos.worker

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.id.WorkId
import io.chronos.{Job, JobSpec, Work}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class ExecuteWork(work: Work)
  case class FailedWork(workId: WorkId, cause: Throwable)
  case class CompletedWork(workId: WorkId, result: Any)
  
  def props = Props[JobExecutor]
}

class JobExecutor extends Actor with ActorLogging {
  import JobExecutor._

  def receive = {
    case ExecuteWork(work) =>
      log.info("Executing work. workId={}", work.id)
      val jobInstance = work.jobClass.newInstance()

      populateJobParams(work.jobClass, work.params, jobInstance)

      try {
        val result = jobInstance.execute()
        sender() ! CompletedWork(work.id, result)
      } catch {
        case cause: Throwable =>
          sender() ! FailedWork(work.id, cause)
      }
  }

  private def populateJobParams[T <: Job](jobSpec: JobSpec, params: Map[String, Any], jobInstance: T): Unit = {
    jobSpec.getDeclaredFields.
      filter(field => params.contains(field.getName)).
      foreach { field =>
        val paramValue = params(field.getName)
        field.set(jobInstance, paramValue)
      }
  }

}
