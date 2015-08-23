package io.chronos.resolver

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.id.ModuleId
import io.chronos.protocol.ResolutionFailed

import scala.concurrent._
import scala.util.{Failure, Success}

/**
 * Created by aalonsodominguez on 22/08/15.
 */
object ModuleResolver {

  type Resolve = (ModuleId, Boolean) => Either[ResolutionFailed, JobPackage]

  def props(resolve: Resolve) = Props(classOf[ModuleResolver], resolve)

  case class ResolveModule(moduleId: ModuleId, download: Boolean = false)
  case class ErrorResolvingModule(moduleId: ModuleId, cause: Throwable)

}

class ModuleResolver(resolve: ModuleResolver.Resolve) extends Actor with ActorLogging {

  import ModuleResolver._
  import context.dispatcher

  def receive: Receive = {
    case ResolveModule(moduleId, download) =>
      val origSender = sender()
      Future {
        resolve(moduleId, download)
      } onComplete {
        case Success(result) =>
          origSender ! result.fold(identity, identity)
        case Failure(error) =>
          origSender ! ErrorResolvingModule(moduleId, error)
      }
  }

}
