package io.kairos.resolver

import akka.actor.{Actor, ActorLogging, Props}
import io.kairos.id.ArtifactId
import io.kairos.protocol.ResolutionFailed

import scala.concurrent._
import scala.util.{Failure, Success}

/**
 * Created by aalonsodominguez on 22/08/15.
 */
object Resolver {

  def props(resolve: Resolve) = Props(classOf[Resolver], resolve)

  case class Validate(moduleId: ArtifactId)
  case class Acquire(moduleId: ArtifactId)
  case class ErrorResolvingModule(moduleId: ArtifactId, cause: Throwable)

}

class Resolver(resolve: Resolve) extends Actor with ActorLogging {

  import Resolver._
  import context.dispatcher

  def receive: Receive = {
    case Validate(moduleId) =>
      val origSender = sender()
      doResolve(moduleId, download = false) onComplete {
        case Success(result) =>
          origSender ! result.fold(identity, identity)

        case Failure(error) =>
          origSender ! ErrorResolvingModule(moduleId, error)
      }

    case Acquire(moduleId) =>
      val origSender = sender()
      doResolve(moduleId, download = true) onComplete {
        case Success(result) =>
          origSender ! result.fold(identity, identity)

        case Failure(error) =>
          origSender ! ErrorResolvingModule(moduleId, error)
      }
  }

  private def doResolve(moduleId: ArtifactId, download: Boolean): Future[Either[ResolutionFailed, Artifact]] = Future {
    resolve(moduleId, download)
  }
  
}
