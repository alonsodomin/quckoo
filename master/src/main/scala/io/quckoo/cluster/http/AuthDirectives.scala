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

import cats.data.OptionT
import cats.instances.future._
import cats.syntax.either._

import io.quckoo.auth.{Passport, Principal}
import io.quckoo.auth.http._
import io.quckoo.cluster.core.Auth

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * Created by alonsodomin on 14/10/2015.
  */
trait AuthDirectives { auth: Auth =>
  import Directives._

  private[this] val QuckooHttpChallenge = HttpChallenge("FormBased", Realm)

  private[this] def authenticationResult[T](result: => Future[Option[T]])(
      implicit ec: ExecutionContext
  ): Future[AuthenticationResult[T]] = {
    OptionT(result)
      .map(_.asRight[HttpChallenge])
      .getOrElse(QuckooHttpChallenge.asLeft[T])
  }

  def authenticateUser(implicit timeout: FiniteDuration): Route = {
    def basicHttpAuth(creds: Option[BasicHttpCredentials])(
        implicit ec: ExecutionContext
    ): Future[AuthenticationResult[Principal]] = {
      authenticationResult(auth.basic(Credentials(creds)))
    }

    extractExecutionContext { implicit ec =>
      implicit val ev = implicitly[ClassTag[BasicHttpCredentials]]
      val authenticate =
        authenticateOrRejectWithChallenge[BasicHttpCredentials, Principal](basicHttpAuth)
      authenticate { (principal: Principal) =>
        completeWithPassport(auth.generatePassport(principal))
      }
    }
  }

  def refreshPassport(implicit timeout: FiniteDuration): Route =
    extractPassport(acceptExpired = true)(completeWithPassport)

  def authenticated: Directive1[Passport] =
    extractPassport(acceptExpired = false).flatMap(provide)

  private[this] def extractPassport(
      acceptExpired: Boolean = false): AuthenticationDirective[Passport] = {
    def oauth2Http(creds: Option[OAuth2BearerToken])(
        implicit ec: ExecutionContext): Future[AuthenticationResult[Passport]] = {
      authenticationResult(auth.bearer(acceptExpired)(Credentials(creds)))
    }

    extractExecutionContext.flatMap { implicit ec =>
      implicit val ev = implicitly[ClassTag[OAuth2BearerToken]]
      authenticateOrRejectWithChallenge[OAuth2BearerToken, Passport](oauth2Http _)
    }
  }

  private[this] def completeWithPassport(passport: Passport): Route =
    complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, passport.token))

  def invalidateAuth: Directive0 =
    setCookie(HttpCookie(AuthCookie, "", path = Some("/"), expires = Some(DateTime.now)))

}
