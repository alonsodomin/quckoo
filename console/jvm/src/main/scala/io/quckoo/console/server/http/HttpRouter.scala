package io.quckoo.console.server.http

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, ValidationRejection}
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkasse.EventStreamMarshalling
import io.quckoo.console.server.core.Server
import io.quckoo.protocol.SchedulerProtocol
import io.quckoo.serialization

trait HttpRouter extends RegistryHttpRouter with SchedulerHttpRouter with AuthDirectives with EventStreamMarshalling {
  this: Server =>

  import StatusCodes._
  import SchedulerProtocol._
  import serialization.json.jvm._

  private[this] def defineApi(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    path("login") {
      post { authenticateRequest }
    } ~ authorizeRequest {
      path("logout") {
        post {
          invalidateAuth {
            complete(OK)
          }
        }
      } ~ pathPrefix("cluster") {
        path("events") {
          get {
            extractExecutionContext { implicit ec =>
              complete(events)
            }
          }
        } ~ path("info") {
          get {
            extractExecutionContext { implicit ec =>
              complete(clusterDetails)
            }
          }
        }
      } ~ pathPrefix("registry") {
        registryApi
      } ~ pathPrefix("scheduler") {
        schedulerApi
      }
    }

  private[this] def staticResources: Route = get {
    pathSingleSlash {
      getFromResource("kairos/index.html")
    } ~ getFromResourceDirectory("kairos")
  }

  private[this] def exceptionHandler(log: LoggingAdapter) = ExceptionHandler {
    case exception =>
      extractUri { uri =>
        log.error(exception, s"Request to URI '$uri' threw exception.")
        complete(HttpResponse(InternalServerError, entity = exception.getMessage))
      }
  }

  private[this] def rejectionHandler(log: LoggingAdapter) = RejectionHandler.newBuilder().
    handle { case ValidationRejection(msg, cause) =>
        log.error(s"$msg - reason: $cause")
        complete(HttpResponse(BadRequest, entity = msg))
    } result()

  def router(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    logRequest("HTTPRequest", Logging.InfoLevel) {
      logResult("HTTPResponse", Logging.InfoLevel) {
        handleExceptions(exceptionHandler(system.log)) {
          handleRejections(rejectionHandler(system.log)) {
            pathPrefix("api") {
              defineApi
            } ~ staticResources
          }
        }
      }
    }

}
