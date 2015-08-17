package io.chronos.worker

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.cluster.Task
import io.chronos.resolver.ModuleResolver

import scala.util.{Failure, Success, Try}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class Execute(work: Task)

  case class Failed(executionId: UUID, reason: TaskFailedCause)
  case class Completed(executionId: UUID, result: Any)

  def props(moduleResolver: ModuleResolver): Props =
    Props(classOf[JobExecutor], moduleResolver)
}

class JobExecutor(val moduleResolver: ModuleResolver) extends Actor with ActorLogging {
  import JobExecutor._

  def receive = {
    case Execute(work) =>
      moduleResolver.resolve(work.moduleId, download = true) match {
        case Right(jobPackage) =>
          log.info("Executing work. workId={}", work.executionId)
          jobPackage.newJob(work.jobClass, work.params) flatMap { job => Try(job.call()) } match {
            case Success(result) =>
              sender() ! Completed(work.executionId, result)
            case Failure(cause) =>
              sender() ! Failed(work.executionId, Right(cause))
          }

        case Left(resolutionFailed) =>
          sender() ! Failed(work.executionId, Left(resolutionFailed))
      }
  }

}
