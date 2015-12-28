package io.kairos.worker

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.kairos.cluster.{Task, TaskFailureCause}
import io.kairos.protocol.ResolutionFailed
import io.kairos.resolver.{Artifact, Resolver}
import io.kairos.worker.JobExecutor.{Completed, Failed}

import scala.util.{Failure, Success, Try}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class Execute(task: Task)

  case class Failed(reason: TaskFailureCause)
  case class Completed(result: Any)

  def props(resolver: ActorRef): Props =
    Props(classOf[JobExecutor], resolver)
}

class JobExecutor(resolver: ActorRef) extends Actor with ActorLogging {
  import JobExecutor._
  import Resolver._

  def receive = {
    case Execute(task) =>
      val runner = context.actorOf(Props(classOf[JobRunner], task, context.parent))
      resolver.tell(Acquire(task.artifactId), runner)
  }

}

private class JobRunner(task: Task, worker: ActorRef) extends Actor with ActorLogging {
  import Resolver._
  
  def receive: Receive = {
    case pkg: Artifact =>
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
