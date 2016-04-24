/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster.http

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives._

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
  import Directives._
  //import RouteDirectives._
  import AuthenticationFailedRejection._

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
    authenticateToken(acceptExpired = false).recoverPF {
      case AuthenticationFailedRejection(CredentialsMissing, _) :: _ => authenticateTokenCookie
    } flatMap(provide(_))

  private[this] def authenticate0[C <: HttpCredentials: ClassTag, U](
    challengeScheme: String,
    authenticator: Credentials => Future[Option[U]]
  ): AuthenticationDirective[U] = {
    extractExecutionContext.flatMap { implicit ec =>
      authenticateOrRejectWithChallenge[C, U] { cred ⇒
        authenticator(Credentials(cred)).map {
          case Some(u) => AuthenticationResult.success(u)
          case None => AuthenticationResult.failWithChallenge(HttpChallenge(challengeScheme, auth.Realm))
        }
      }
    }
  }

  def authenticateWithCookie[U](
    challengeScheme: String,
    authenticator: Credentials => Future[Option[U]]
  ): AuthenticationDirective[U] = {
    extractExecutionContext.flatMap { implicit ec =>
      optionalCookie(AuthCookie).flatMap { authCookie =>
        val creds = Credentials(authCookie.map(cookie => OAuth2BearerToken(cookie.value)))
        val authResult = authenticator(creds).map {
          case Some(u) => AuthenticationResult.success(u)
          case None => AuthenticationResult.failWithChallenge(HttpChallenge(challengeScheme, auth.Realm))
        }
        onSuccess(authResult).flatMap {
          case Right(u) => provide(u)
          case Left(challenge) =>
            val cause = creds match {
              case Credentials.Missing => CredentialsMissing
              case _                   => CredentialsRejected
            }
            reject(AuthenticationFailedRejection(cause, challenge)): Directive1[U]
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

  private[this] def authenticateTokenCookie: AuthenticationDirective[User] = {
    extractExecutionContext.flatMap { implicit ec =>
      authenticateWithCookie("Bearer", auth.token(acceptExpired = false))
    }
  }

  private[this] def completeWithAuthToken(user: User): Route = {
    val jwt = auth.generateToken(user)
    val authCookie = HttpCookie(
      AuthCookie, jwt, path = Some("/"), expires = Some(DateTime.now + 30.minutes.toMillis)
    )
    setCookie(authCookie) {
      complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, jwt))
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
