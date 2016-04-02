package io.quckoo.cluster.http

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, ValidationRejection}
import akka.stream.ActorMaterializer

import de.heikoseeberger.akkasse.{EventStreamMarshalling, ServerSentEvent}

import io.quckoo.cluster.core.QuckooServer
import io.quckoo.cluster.registry.RegistryHttpRouter
import io.quckoo.cluster.scheduler.SchedulerHttpRouter
import io.quckoo.protocol.worker.WorkerEvent
import upickle.default._

import scala.concurrent.duration._

trait HttpRouter extends RegistryHttpRouter with SchedulerHttpRouter with AuthDirectives with EventStreamMarshalling {
  this: QuckooServer =>

  import StatusCodes._

  final val ResourcesDir = "quckoo"

  private[this] def defineApi(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    pathPrefix("auth") {
      path("login") {
        post {
          authenticateUser
        }
      } ~ path("refresh") {
        get {
          refreshToken
        }
      }
    } ~ authenticated { user =>
      path("logout") {
        post {
          invalidateAuth {
            complete(OK)
          }
        }
      } ~ pathPrefix("cluster") {
        path("info") {
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
    } ~ path("workers" / "events") {
      get {
        complete {
          workerEvents.map { event =>
            ServerSentEvent(write[WorkerEvent](event), "worker")
          } keepAlive(1 second, () => ServerSentEvent.heartbeat)
        }
      }
    }

  private[this] def staticResources: Route = get {
    pathSingleSlash {
      getFromResource(s"$ResourcesDir/index.html")
    } ~ getFromResourceDirectory(ResourcesDir)
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
