package io.chronos.worker

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.id.ExecutionId
import io.chronos.{Job, JobClass, Work}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class ExecuteWork(work: Work)
  case class FailedWork(workId: ExecutionId, cause: Throwable)
  case class CompletedWork(workId: ExecutionId, result: Any)
  
  def props = Props[JobExecutor]
}

class JobExecutor extends Actor with ActorLogging {
  import JobExecutor._

  def receive = {
    case ExecuteWork(work) =>
      log.info("Executing work. workId={}", work.executionId)
      val jobInstance = work.jobClass.newInstance()

      populateJobParams(work.jobClass, work.params, jobInstance)

      try {
        val result = jobInstance.execute()
        sender() ! CompletedWork(work.executionId, result)
      } catch {
        case cause: Throwable =>
          sender() ! FailedWork(work.executionId, cause)
      }
  }

  private def populateJobParams[T <: Job](jobClass: JobClass, params: Map[String, Any], jobInstance: T): Unit = {
    jobClass.getDeclaredFields.
      filter(field => params.contains(field.getName)).
      foreach { field =>
        val paramValue = params(field.getName)
        field.set(jobInstance, paramValue)
      }
  }

}
