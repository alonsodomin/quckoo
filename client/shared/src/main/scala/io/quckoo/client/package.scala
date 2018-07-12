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

package io.quckoo

import cats._
import cats.data._
import cats.effect._
import cats.implicits._

import io.quckoo.auth.{Passport, Unauthorized}

import scala.concurrent.Future

package object client {
  import NewQuckooClient.ClientState

  type ClientIO[A] = StateT[IO, ClientState, A]
  object ClientIO {
    def pure[A](a: A): ClientIO[A] =
      StateT.pure[IO, ClientState, A](a)

    def fromFuture[A](action: IO[Future[A]]): ClientIO[A] =
      StateT.liftF(IO.fromFuture(action))

    def fromEither[A](result: Either[Throwable, A]): ClientIO[A] =
      StateT.liftF(IO.fromEither(result))

    def getPassport: ClientIO[Passport] = {
      def retrievePassport(state: ClientState): IO[Passport] = state.passport match {
        case Some(pass) => IO.pure(pass)
        case _          => IO.raiseError(Unauthorized)
      }

      for {
        state    <- StateT.get[IO, ClientState]
        passport <- StateT.liftF(retrievePassport(state))
      } yield passport
    }

    def setPassport(passport: Passport): ClientIO[Unit] =
      StateT.set(ClientState(passport.some))
  }
}