package io.kairos.ui

import java.util.UUID

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, ValidationRejection}
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpupickle.UpickleSupport
import io.kairos.ui.protocol._

trait HttpRouter extends UpickleSupport {

  import StatusCodes._

  private[this] def api(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    path("login") {
      post {
        entity(as[LoginRequest]) { req =>
          val token = UUID.randomUUID().toString
          setCookie(HttpCookie(Cookies.AuthTokenName, token, path = Some("/"))) {
            complete(LoginResponse(token))
          }
        }
      }
    } ~ path("cluster") {
      complete(ClusterDetails())
    }

  private[this] def static: Route =
    get {
      pathSingleSlash {
        complete {
          HttpEntity(MediaTypes.`text/html`, ClientBootstrap.skeleton.render)
        }
      } ~ getFromResourceDirectory("")
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
        complete(Unauthorized, msg)
    } result()

  def router(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    logRequest("HTTPRequest") {
      logResult("HTTPResponse") {
        handleExceptions(exceptionHandler(system.log)) {
          handleRejections(rejectionHandler(system.log)) {
            static ~ pathPrefix("api") {
              api
            }
          }
        }
      }
    }

}
