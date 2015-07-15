package io.chronos.worker

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.id.ExecutionId
import io.chronos.{Job, JobClass, Work}

import scala.util.{Failure, Success, Try}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class Execute(work: Work)
  case class Failed(executionId: ExecutionId, cause: Throwable)
  case class Completed(executionId: ExecutionId, result: Any)
  
  def props = Props[JobExecutor]
}

class JobExecutor extends Actor with ActorLogging {
  import JobExecutor._

  def receive = {
    case Execute(work) =>
      log.info("Executing work. workId={}", work.executionId)
      val jobInstance = work.jobClass.newInstance()

      populateJobParams(work.jobClass, work.params, jobInstance)

      Try[Any](jobInstance.execute()) match {
        case Success(result) =>
          sender() ! Completed(work.executionId, result)
        case Failure(cause) =>
          sender() ! Failed(work.executionId, cause)
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
