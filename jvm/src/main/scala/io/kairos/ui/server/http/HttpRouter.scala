package io.kairos.ui.server.http

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, ValidationRejection}
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpupickle.UpickleSupport
import io.kairos.ui.protocol._
import io.kairos.ui.server.ServerFacade
import io.kairos.ui.server.boot.ClientBootstrap

trait HttpRouter extends UpickleSupport with AuthDirectives {
  this: ServerFacade =>

  import StatusCodes._

  private[this] def defineApi(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    path("login") {
      post { authenticateRequest }
    } ~ authorizeRequest {
      path("cluster") {
        get {
          complete(ClusterDetails())
        }
      }
    }

  private[this] def staticResources: Route =
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
            staticResources ~ pathPrefix("api") {
              defineApi
            }
          }
        }
      }
    }

}
