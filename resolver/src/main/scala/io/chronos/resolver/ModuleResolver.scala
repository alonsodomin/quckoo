package io.chronos.resolver

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.id.ModuleId

import scala.concurrent._
import scala.util.{Failure, Success}

/**
 * Created by aalonsodominguez on 22/08/15.
 */
object ModuleResolver {

  def props(dependencyResolver: DependencyResolver) =
    Props(classOf[ModuleResolver], dependencyResolver)

  case class ResolveModule(moduleId: ModuleId, download: Boolean = false)
  case class ErrorResolvingModule(moduleId: ModuleId, cause: Throwable)

}

class ModuleResolver(dependencyResolver: DependencyResolver) extends Actor with ActorLogging {

  import ModuleResolver._
  import context.dispatcher

  def receive: Receive = {
    case ResolveModule(moduleId, download) =>
      val origSender = sender()
      Future {
        dependencyResolver.resolve(moduleId, download)
      } onComplete {
        case Success(result) =>
          def sendResponse(value: Any) = origSender ! value
          result.fold(sendResponse, sendResponse)
        case Failure(error) =>
          origSender ! ErrorResolvingModule(moduleId, error)
      }
  }

}
