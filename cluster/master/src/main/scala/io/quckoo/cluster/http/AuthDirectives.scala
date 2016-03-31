package io.quckoo.cluster.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{HttpCookie, OAuth2BearerToken}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.AuthenticationDirective
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpupickle.UpickleSupport
import io.quckoo.auth._
import io.quckoo.auth.http._
import io.quckoo.cluster.core.Auth
import io.quckoo.protocol.client.SignIn

import scala.concurrent.duration._

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait AuthDirectives extends UpickleSupport { auth: Auth =>
  import StatusCodes._

  def extractAuthInfo: Directive1[XSRFToken] =
    headerValueByName(XSRFTokenHeader).flatMap { header =>
      provide(XSRFToken(header))
    }

  def authenticateUser: Route = {
    extractExecutionContext { implicit ec =>
      authenticateBasicAsync[User](realm = auth.Realm, auth.authenticateCreds)(completeWithAuthToken)
    }
  }

  def refreshToken: Route = {
    extractExecutionContext { implicit ec =>
      authenticateOAuth2Async[User](realm = Realm, auth.authenticateToken(acceptExpired = true))(completeWithAuthToken)
    }
  }

  def authenticateRequest: AuthenticationDirective[User] = {
    extractExecutionContext.flatMap { implicit ec =>
      authenticateOAuth2Async[User](realm = Realm, auth.authenticateToken(acceptExpired = false))
    }
  }

  private[this] def completeWithAuthToken(user: User): Route = {
    val jwt = auth.generateToken(user)
    val authCookie = HttpCookie(
      "X-Auth-Token", jwt, path = Some("/"), expires = Some(DateTime.now + 30.minutes.toMillis)
    )
    setCookie(authCookie) {
      complete(OK)
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

  // TODO remove this method
  def refreshAuthInfo: Directive0 =
    extractAuthInfo.flatMap { authInfo =>
      addAuthCookies(authInfo)
    }

  private[this] def addAuthCookies(auth: XSRFToken): Directive0 =
    setCookie(HttpCookie(
      XSRFTokenCookie, auth.toString, path = Some("/"), expires = Some(DateTime.now + 30.minutes.toMillis)
    ))

}
