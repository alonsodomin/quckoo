package io.chronos.resolver

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.client.ClusterClientReceptionist
import io.chronos.id.ModuleId
import io.chronos.protocol.ResolutionFailed

import scala.concurrent._
import scala.util.{Failure, Success}

/**
 * Created by aalonsodominguez on 22/08/15.
 */
object Resolver {

  def props(resolve: ResolveFun) = Props(classOf[Resolver], resolve)

  case class Validate(moduleId: ModuleId)
  case class Resolve(moduleId: ModuleId)
  case class ErrorResolvingModule(moduleId: ModuleId, cause: Throwable)

}

class Resolver(resolve: ResolveFun) extends Actor with ActorLogging {

  import Resolver._
  import context.dispatcher

  ClusterClientReceptionist(context.system).registerService(self)

  def receive: Receive = {
    case Validate(moduleId) =>
      val origSender = sender()
      doResolve(moduleId, download = false) onComplete {
        case Success(result) =>
          origSender ! result.fold(identity, identity)

        case Failure(error) =>
          origSender ! ErrorResolvingModule(moduleId, error)
      }

    case Resolve(moduleId) =>
      val origSender = sender()
      doResolve(moduleId, download = true) onComplete {
        case Success(result) =>
          origSender ! result.fold(identity, identity)

        case Failure(error) =>
          origSender ! ErrorResolvingModule(moduleId, error)
      }
  }

  private def doResolve(moduleId: ModuleId, download: Boolean): Future[Either[ResolutionFailed, JobPackage]] = Future {
    resolve(moduleId, download)
  }
  
}
