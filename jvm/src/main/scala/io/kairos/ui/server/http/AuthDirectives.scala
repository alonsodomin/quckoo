package io.kairos.ui.server.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpupickle.UpickleSupport
import io.kairos.ui.auth.Auth
import io.kairos.ui.protocol.LoginRequest
import io.kairos.ui.server.security.{AuthInfo, SecurityFacade}

import scala.concurrent.duration._

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait AuthDirectives extends UpickleSupport { auth: SecurityFacade =>
  import StatusCodes._

  def extractAuthInfo: Directive1[AuthInfo] =
    headerValueByName(Auth.XSRFTokenHeader).flatMap { header =>
      provide(AuthInfo(header))
    }

  def authenticateRequest(implicit system: ActorSystem, materizalizer: ActorMaterializer): Route =
    entity(as[LoginRequest]) { req =>
      extractExecutionContext { implicit ec =>
        onSuccess(auth.authenticate(req.username, req.password.toCharArray)) {
          case Some(authInfo) =>
            addAuthCookies(authInfo) {
              complete(OK)
            }
          case _ =>
            complete(Unauthorized)
        }
      }
    }

  def authorizeRequest: Directive0 = {
    optionalCookie(Auth.XSRFTokenCookie).flatMap {
      case Some(cookie) =>
        headerValueByName(Auth.XSRFTokenHeader).flatMap { header =>
          if (header != cookie.value) {
            reject(AuthorizationFailedRejection)
          } else pass & cancelRejection(AuthorizationFailedRejection)
        }
      case None =>
        reject(AuthorizationFailedRejection)
    }
  }

  def refreshAuthInfo: Directive0 =
    extractAuthInfo.flatMap { authInfo =>
      addAuthCookies(authInfo.refresh())
    }

  private[this] def addAuthCookies(auth: AuthInfo): Directive0 =
    setCookie(HttpCookie(
      Auth.XSRFTokenCookie, auth.token, path = Some("/"), expires = Some(DateTime.now + 30.minutes.toMillis)
    ))

}
