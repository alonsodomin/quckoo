package io.chronos.worker

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.cluster.{Task, TaskFailureCause}
import io.chronos.resolver.DependencyResolver

import scala.util.{Failure, Success, Try}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class Execute(task: Task)

  case class Failed(reason: TaskFailureCause)
  case class Completed(result: Any)

  def props(moduleResolver: DependencyResolver): Props =
    Props(classOf[JobExecutor], moduleResolver)
}

class JobExecutor(val moduleResolver: DependencyResolver) extends Actor with ActorLogging {
  import JobExecutor._

  def receive = {
    case Execute(task) =>
      moduleResolver.resolve(task.moduleId, download = true) match {
        case Right(jobPackage) =>
          log.info("Executing task. taskId={}", task.id)
          jobPackage.newJob(task.jobClass, task.params) flatMap { job => Try(job.call()) } match {
            case Success(result) =>
              sender() ! Completed(result)
            case Failure(cause) =>
              sender() ! Failed(Right(cause))
          }

        case Left(resolutionFailed) =>
          sender() ! Failed(Left(resolutionFailed))
      }
  }

}
