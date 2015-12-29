package io.kairos.worker

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.kairos.cluster.Task
import io.kairos.protocol.{Error, ExceptionThrown}
import io.kairos.resolver.{Artifact, ResolutionResult, Resolver}
import io.kairos.worker.JobExecutor.{Completed, Failed}

import scala.util.Try
import scalaz.Scalaz._
import scalaz.{ValidationNel, _}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class Execute(task: Task)

  case class Failed(errors: NonEmptyList[Error])
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
  
  def receive: Receive = {
    case result: ResolutionResult =>
      import scala.util.{Failure, Success}
      reply(result.map(runJob).flatMap {
        case Success(r)     => r.successNel[Error]
        case Failure(cause) => ExceptionThrown(cause).failureNel[Any]
      })
  }

  private[this] def runJob(artifact: Artifact): Try[Any] =
    artifact.newJob(task.jobClass, task.params) flatMap { job => Try(job.call()) }

  private[this] def reply(msg: ValidationNel[Error, Any]): Unit = {
    val response = msg match {
      case Success(value)  => Completed(value)
      case Failure(errors) => Failed(errors)
    }

    worker ! response
    context.stop(self)
  }
  
}
