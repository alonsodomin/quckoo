package io.quckoo.cluster.http

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{AuthenticationDirective, AuthenticationResult, Credentials}

import de.heikoseeberger.akkahttpupickle.UpickleSupport

import io.quckoo.auth._
import io.quckoo.auth.http._
import io.quckoo.cluster.core.Auth

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait AuthDirectives extends UpickleSupport { auth: Auth =>

  def extractAuthInfo: Directive1[XSRFToken] =
    headerValueByName(XSRFTokenHeader).flatMap { header =>
      provide(XSRFToken(header))
    }

  def authenticateUser: Route =
    extractExecutionContext { implicit ec =>
      implicit val ev = implicitly[ClassTag[BasicHttpCredentials]]
      val directive = authenticate0[BasicHttpCredentials, User]("FormBased", auth.basic)
      directive { completeWithAuthToken(_) }
    }

  def refreshToken: Route =
    authenticateToken(acceptExpired = true)(completeWithAuthToken)

  def authenticated: Directive1[User] =
    authenticateToken(acceptExpired = false) flatMap (provide(_))

  private[this] def authenticate0[C <: HttpCredentials: ClassTag, U](
    challengeScheme: String,
    authenticator: Credentials => Future[Option[U]]
  ): AuthenticationDirective[U] = {
    extractExecutionContext.flatMap { implicit ec =>
      authenticateOrRejectWithChallenge[C, U] { cred ⇒
        authenticator(Credentials(cred)).map {
          case Some(u) => AuthenticationResult.success(u)
          case None    => AuthenticationResult.failWithChallenge(HttpChallenge(challengeScheme, auth.Realm))
        }
      }
    }
  }

  private[this] def authenticateToken(acceptExpired: Boolean = false): AuthenticationDirective[User] = {
    extractExecutionContext.flatMap { implicit ec =>
      implicit val ev = implicitly[ClassTag[OAuth2BearerToken]]
      authenticate0[OAuth2BearerToken, User]("Bearer", auth.token(acceptExpired))
    }
  }

  private[this] def completeWithAuthToken(user: User): Route = {
    val jwt = auth.generateToken(user)
    val authCookie = HttpCookie(
      AuthCookie, jwt, path = Some("/"), expires = Some(DateTime.now + 30.minutes.toMillis)
    )
    setCookie(authCookie) {
      complete(jwt)
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
    setCookie(HttpCookie(AuthCookie, "", path = Some("/"), expires = Some(DateTime.now)))

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
