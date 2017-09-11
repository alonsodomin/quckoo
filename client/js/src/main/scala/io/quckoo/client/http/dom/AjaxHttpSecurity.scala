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

package io.quckoo.client.http.dom

import cats.Eval
import cats.effect.IO
import cats.implicits._

import io.quckoo.api2.{Security, QuckooIO}
import io.quckoo.auth._
import io.quckoo.client.http._

import org.scalajs.dom
import org.scalajs.dom.ext.{Ajax, AjaxException}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

trait AjaxHttpSecurity extends Security[QuckooIO] {

  private def readPassportFromResponse(request: dom.XMLHttpRequest): IO[Passport] =
    if (request.status == 200)
      IO.async(cb => cb(Passport(request.responseText)))
    else IO.raiseError(InvalidCredentials)

  override def signIn(username: String, password: String): QuckooIO[Unit] = {
    def authHeaders(credentials: Credentials): IO[Map[String, String]] =
      credentials.toBase64.map(v => Map(AuthorizationHeader -> v))

    def authenticate(): IO[Session.Authenticated] = {
      val passport = for {
        creds    <- IO.pure(Credentials(username, password))
        headers  <- authHeaders(creds)
        request  <- IO.fromFuture(Eval.later(Ajax.post(LoginURI, headers = headers)))
        passport <- readPassportFromResponse(request)
      } yield passport

      passport
        .map(Session.Authenticated)
        .recoverWith {
          case err: AjaxException if err.xhr.status == 401 =>
            IO.raiseError(InvalidCredentials)
        }
    }

    QuckooIO.session {
      case Session.Anonymous           => authenticate()
      case auth: Session.Authenticated => IO.pure(auth)
    }
  }

  override def signOut(): QuckooIO[Unit] = QuckooIO.session {
    case anon: Session.Anonymous => IO.pure(anon)
    case Session.Authenticated(passport) =>
      val req =
        IO.fromFuture(Eval.later(Ajax.post(LogoutURI, headers = Map(bearerToken(passport)))))
      req.map(_ => Session.Anonymous)
  }

  override def refreshToken(): QuckooIO[Unit] = QuckooIO.session {
    case Session.Anonymous => IO.raiseError(NotAuthorized)
    case auth @ Session.Authenticated(passport) =>
      val req =
        IO.fromFuture(
          Eval.later(
            Ajax.post(AuthRefreshURI, headers = Map(bearerToken(passport)))
          )
        )

      (req >>= readPassportFromResponse)
        .map(pass => auth.copy(passport = pass))
        .recoverWith {
          case err: AjaxException if (err.xhr.status == 401) || (err.xhr.status == 403) =>
            IO.raiseError(SessionExpired)
        }
  }
}
