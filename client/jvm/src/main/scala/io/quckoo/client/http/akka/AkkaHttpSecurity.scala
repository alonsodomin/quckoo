/*
 * Copyright 2015 A. Alonso Dominguez
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

package io.quckoo.client.http.akka

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._

import cats.effect.IO

import io.quckoo.api2.{QuckooIO, Security}
import io.quckoo.auth._
import io.quckoo.client.http._

trait AkkaHttpSecurity extends AkkaHttpClientSupport with Security[QuckooIO] {
  import ContentTypes.`text/plain(UTF-8)`

  private def readPassportFromResponse(response: HttpResponse): IO[Passport] =
    response.entity match {
      case HttpEntity.Strict(`text/plain(UTF-8)`, data) =>
        IO.async { callback =>
          callback(Passport(data.utf8String))
        }

      case _ => IO.raiseError(InvalidCredentials)
    }

  override def signIn(username: String, password: String): QuckooIO[Unit] = {
    def authenticate(): IO[Session.Authenticated] = {
      val credentials = BasicHttpCredentials(username, password)
      val request = HttpRequest(HttpMethods.POST, uri = LoginURI)
        .withHeaders(Authorization(credentials))

      sendRequest(request) {
        case res if res.status == StatusCodes.Unauthorized =>
          IO.raiseError(InvalidCredentials)
        case res if res.status == StatusCodes.Forbidden =>
          IO.raiseError(InvalidCredentials)

        case res if res.status == StatusCodes.OK =>
          readPassportFromResponse(res).map { passport =>
            Session.Authenticated(passport)
          }
      }
    }

    QuckooIO.session {
      case Session.Anonymous           => authenticate()
      case auth: Session.Authenticated => IO.pure(auth)
    }
  }

  override def signOut(): QuckooIO[Unit] = QuckooIO.session {
    case anon: Session.Anonymous => IO.pure(anon)
    case auth: Session.Authenticated =>
      val request =
        HttpRequest(HttpMethods.POST, uri = LogoutURI).withSession(auth)
      sendRequest(request) {
        case res if res.status.isSuccess() => IO.pure(Session.Anonymous)
      }
  }

  override def refreshToken(): QuckooIO[Unit] = QuckooIO.session {
    case Session.Anonymous => IO.raiseError(NotAuthorized)
    case auth: Session.Authenticated =>
      val request =
        HttpRequest(HttpMethods.POST, uri = AuthRefreshURI).withSession(auth)
      sendRequest(request) {
        case res if res.status == StatusCodes.Forbidden =>
          IO.raiseError(SessionExpired)
        case res if res.status == StatusCodes.Unauthorized =>
          IO.raiseError(SessionExpired)

        case res if res.status == StatusCodes.OK =>
          readPassportFromResponse(res).map { pass =>
            auth.copy(passport = pass)
          }
      }
  }
}
