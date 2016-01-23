package io.kairos.worker

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern._
import io.kairos._
import io.kairos.cluster.Task
import io.kairos.resolver.{Artifact, Resolve}

import scala.util.Try
import scala.util.control.NonFatal
import scalaz._

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class Execute(task: Task)

  case class Failed(errors: Faults)
  case class Completed(result: Any)

  def props(resolve: Resolve): Props =
    Props(classOf[JobExecutor], resolve)
}

class JobExecutor(resolve: Resolve) extends Actor with ActorLogging {
  import JobExecutor._

  import Scalaz._

  def receive = {
    case Execute(task) =>
      import context.dispatcher

      resolve(task.artifactId, download = true) recover {
        case NonFatal(ex) => ExceptionThrown(ex).failureNel[Artifact]
      } map {
        _.disjunctioned {
          import scala.util.{Failure, Success}

          _.flatMap(artifact => runTaskFrom(artifact, task) match {
            case Success(result) => \/-(result)
            case Failure(cause)  => -\/(NonEmptyList(ExceptionThrown(cause)))
          })
        }
      } map {
        case Success(value)  => Completed(value)
        case Failure(errors) => Failed(errors)
      } pipeTo sender()
  }

  private[this] def runTaskFrom(artifact: Artifact, task: Task): Try[Any] =
    artifact.newJob(task.jobClass, task.params) flatMap { job => Try(job.call()) }

}

