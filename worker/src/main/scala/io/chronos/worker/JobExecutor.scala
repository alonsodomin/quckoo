package io.chronos.worker

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.chronos.cluster.{Task, TaskFailureCause}
import io.chronos.protocol.ResolutionFailed
import io.chronos.resolver.{ChronosResolver, JobPackage, ModuleResolver}
import io.chronos.worker.JobExecutor.{Completed, Failed}

import scala.util.{Failure, Success, Try}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class Execute(task: Task)

  case class Failed(reason: TaskFailureCause)
  case class Completed(result: Any)

  def props(dependencyResolver: ChronosResolver): Props =
    Props(classOf[JobExecutor], dependencyResolver)
}

class JobExecutor(dependencyResolver: ChronosResolver) extends Actor with ActorLogging {
  import JobExecutor._
  import ModuleResolver._

  private val moduleResolver = context.actorOf(ModuleResolver.props(dependencyResolver), "moduleResolver")
  
  def receive = {
    case Execute(task) =>
      val runner = context.actorOf(Props(classOf[JobRunner], task, context.parent))
      moduleResolver.tell(ResolveModule(task.moduleId, download = true), runner)
      /*dependencyResolver.resolve(task.moduleId, download = true) match {
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
      }*/
  }

}

private class JobRunner(task: Task, worker: ActorRef) extends Actor with ActorLogging {
  import ModuleResolver._
  
  def receive: Receive = {
    case pkg: JobPackage =>
      log.info("Executing task. taskId={}", task.id)
      pkg.newJob(task.jobClass, task.params) flatMap { job => Try(job.call()) } match {
        case Success(result) =>
          reply(Completed(result)) 
        case Failure(cause) =>
          reply(Failed(Right(cause))) 
      }

    case failed: ResolutionFailed =>
      reply(Failed(Left(failed))) 

    case error: ErrorResolvingModule =>
      reply(Failed(Right(error.cause))) 
  }
  
  private def reply(msg: Any): Unit = {
    worker ! msg
    context.stop(self)
  }
  
}
