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

package io.quckoo.client.http

import cats.data._
import cats.effect._
import cats.implicits._

import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp._
import com.softwaremill.sttp.circe._

import io.quckoo.client._
import io.quckoo.auth.{InvalidCredentials, Passport}

abstract class JVMQuckooClient extends NewQuckooClient {
  import NewQuckooClient._

  implicit val backend = AkkaHttpBackend()

  def signIn(username: String, password: String): ClientIO[Unit] = {
    def decodeLoginBody(body: Either[String, String]): Either[Throwable, Passport] = {
      val invalid: Either[Throwable, String] = body.leftMap(_ => InvalidCredentials)
      invalid >>= Passport.apply
    }

    for {
      request <- ClientIO.pure(
        sttp.post(uri"$LoginURI").auth.basic(username, password)
      )
      response <- ClientIO.fromFuture(IO(request.send()))
      passport <- ClientIO.fromAttempt(decodeLoginBody(response.body))
      _        <- ClientIO.setPassport(passport)
    } yield ()
  }

  def signOut(): ClientIO[Unit] =
    for {
      passport <- ClientIO.getPassport
      request  <- ClientIO.pure(sttp.post(uri"$LogoutURI").auth.bearer(passport.toString))
      response <- ClientIO.fromFuture(IO(request.send()))
      _        <- ClientIO.fromEither(response.body)
    } yield ()

}
