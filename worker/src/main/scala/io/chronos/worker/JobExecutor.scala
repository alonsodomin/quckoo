package io.chronos.worker

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.cluster.{Task, TaskFailureCause}
import io.chronos.resolver.ModuleResolver

import scala.util.{Failure, Success, Try}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class Execute(work: Task)

  case class Failed(executionId: UUID, reason: TaskFailureCause)
  case class Completed(executionId: UUID, result: Any)

  def props(moduleResolver: ModuleResolver): Props =
    Props(classOf[JobExecutor], moduleResolver)
}

class JobExecutor(val moduleResolver: ModuleResolver) extends Actor with ActorLogging {
  import JobExecutor._

  def receive = {
    case Execute(task) =>
      moduleResolver.resolve(task.moduleId, download = true) match {
        case Right(jobPackage) =>
          log.info("Executing task. taskId={}", task.id)
          jobPackage.newJob(task.jobClass, task.params) flatMap { job => Try(job.call()) } match {
            case Success(result) =>
              sender() ! Completed(task.id, result)
            case Failure(cause) =>
              sender() ! Failed(task.id, Right(cause))
          }

        case Left(resolutionFailed) =>
          sender() ! Failed(task.id, Left(resolutionFailed))
      }
  }

}
