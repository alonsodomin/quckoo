package io.kairos.console.server.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpupickle.UpickleSupport
import io.kairos.api.Auth
import io.kairos.auth._
import io.kairos.console.protocol.LoginRequest
import io.kairos.security._

import scala.concurrent.duration._

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait AuthDirectives extends UpickleSupport { auth: Auth =>
  import StatusCodes._

  def extractAuthInfo: Directive1[AuthInfo] =
    headerValueByName(XSRFTokenHeader).flatMap { header =>
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
    optionalCookie(XSRFTokenCookie).flatMap {
      case Some(cookie) =>
        headerValueByName(XSRFTokenHeader).flatMap { header =>
          if (header != cookie.value) {
            reject(AuthorizationFailedRejection)
          } else {
            pass & cancelRejection(AuthorizationFailedRejection) & refreshAuthInfo
          }
        }

      case None =>
        reject(AuthorizationFailedRejection)
    }
  }

  def invalidateAuth: Directive0 =
    extractAuthInfo.flatMap { authInfo =>
      setCookie(HttpCookie(
        XSRFTokenCookie, authInfo.expire().toString, path = Some("/"), expires = Some(DateTime.now)
      ))
    }

  def refreshAuthInfo: Directive0 =
    extractAuthInfo.flatMap { authInfo =>
      addAuthCookies(authInfo.copy(token = generateAuthToken))
    }

  private[this] def addAuthCookies(auth: AuthInfo): Directive0 =
    setCookie(HttpCookie(
      XSRFTokenCookie, auth.toString, path = Some("/"), expires = Some(DateTime.now + 30.minutes.toMillis)
    ))

}
