package io.chronos.worker

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.Work
import io.chronos.id.ExecutionId
import io.chronos.protocol.ExecutionFailedCause
import io.chronos.resolver.JobModuleResolver

import scala.util.{Failure, Success, Try}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class Execute(work: Work)

  case class Failed(executionId: ExecutionId, reason: ExecutionFailedCause)
  case class Completed(executionId: ExecutionId, result: Any)

  def props(moduleResolver: JobModuleResolver): Props =
    Props(classOf[JobExecutor], moduleResolver)
}

class JobExecutor(val moduleResolver: JobModuleResolver) extends Actor with ActorLogging {
  import JobExecutor._

  def receive = {
    case Execute(work) =>
      moduleResolver.resolve(work.moduleId, download = true) match {
        case Left(jobPackage) =>
          log.info("Executing work. workId={}", work.executionId)
          jobPackage.newJob(work.jobClass, work.params) flatMap { job => Try(job.call()) } match {
            case Success(result) =>
              sender() ! Completed(work.executionId, result)
            case Failure(cause) =>
              sender() ! Failed(work.executionId, Right(cause))
          }

        case Right(resolutionFailed) =>
          sender() ! Failed(work.executionId, Left(resolutionFailed))
      }
  }

}
