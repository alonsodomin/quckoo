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

import io.quckoo.auth.Passport
import io.quckoo.auth.http._
import io.quckoo.cluster.core.Auth

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait AuthDirectives extends UpickleSupport { auth: Auth =>
  import Directives._

  def authenticateUser: Route =
    extractExecutionContext { implicit ec =>
      implicit val ev = implicitly[ClassTag[BasicHttpCredentials]]
      val directive = authenticate0[BasicHttpCredentials, Passport]("FormBased", auth.basic)
      directive { completeWithPassport(_) }
    }

  def refreshPassport: Route =
    extractPassport(acceptExpired = true)(completeWithPassport)

  def authenticated: Directive1[Passport] =
    extractPassport(acceptExpired = false).flatMap(provide)

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

  private[this] def extractPassport(acceptExpired: Boolean = false): AuthenticationDirective[Passport] = {
    extractExecutionContext.flatMap { implicit ec =>
      implicit val ev = implicitly[ClassTag[OAuth2BearerToken]]
      authenticate0[OAuth2BearerToken, Passport]("Bearer", auth.passport(acceptExpired))
    }
  }

  private[this] def completeWithPassport(passport: Passport): Route =
    complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, passport.token))

  def invalidateAuth: Directive0 =
    setCookie(HttpCookie(AuthCookie, "", path = Some("/"), expires = Some(DateTime.now)))

}
