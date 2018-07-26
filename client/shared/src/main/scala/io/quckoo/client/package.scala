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

import com.softwaremill.sttp._

import io.quckoo.auth.{Passport, Unauthorized}
import io.quckoo.util.Attempt

import scala.concurrent.Future

package object client {

  type ClientIO[A] = StateT[IO, ClientState, A]
  object ClientIO {
    def pure[A](a: A): ClientIO[A] =
      StateT.pure[IO, ClientState, A](a)

    def unit: ClientIO[Unit] = pure(())

    def auth = for {
      passport <- getPassport
      req <- pure(sttp.auth.bearer(passport.toString))
    } yield req

    def handle[F[_]: Effect, A, S](request: Request[A, S])(
      implicit backend: SttpBackend[F, S]
    ): ClientIO[A] = for {
      response <- fromEffect(request.send())
      body     <- fromEither(response.body)
    } yield body

    def optionalBody[A](response: Response[A]): ClientIO[Option[A]] = {
      if (response.code == 404) pure(none[A])
      else fromEither(response.body.map(_.some))
    }

    def handleAttempt[F[_]: Effect, E <: Throwable, A, S](request: Request[Either[E, A], S])(
      implicit backend: SttpBackend[F, S]
    ): ClientIO[A] = for {
      body   <- handle(request)
      result <- fromAttempt(body)
    } yield result

    def handleNotFound[E <: Throwable, A, B, S](request: Request[Either[E, A], S])(
      onNotFound: => B,
      onFound: A => B
    )(
      implicit backend: SttpBackend[Future, S]
    ): ClientIO[B] = {
      def optionalBody(response: Response[Either[E, A]]): ClientIO[Either[E, B]] = {
        if (response.code == 404) {
          pure(onNotFound.asRight[E])
        } else {
          fromEither(response.body.map(_.map(onFound)))
        }
      }

      for {
        response <- fromFuture(request.send())
        body     <- optionalBody(response)
        result   <- fromAttempt(body)
      } yield result
    }

    def handleNotFoundEither[E <: Throwable, A, B, S](request: Request[Either[E, A], S])(
      onNotFound: => B
    )(
      implicit backend: SttpBackend[Future, S]
    ): ClientIO[Either[B, A]] =
      handleNotFound(request)(onNotFound.asLeft[A], _.asRight[B])

    def handleNotFoundOption[E <: Throwable, A, S](request: Request[Either[E, A], S])(
      implicit backend: SttpBackend[Future, S]
    ): ClientIO[Option[A]] =
      handleNotFound(request)(none[A], _.some)

    def fromFuture[A](action: => Future[A]): ClientIO[A] =
      StateT.liftF(IO.fromFuture(IO(action)))

    def fromEffect[F[_], A](effect: F[A])(implicit F: Effect[F]): ClientIO[A] =
      StateT.liftF(F.toIO(effect))

    def fromAttempt[A](result: Attempt[A]): ClientIO[A] =
      StateT.liftF(IO.fromEither(result))

    def fromEither[E: Show, A](result: Either[E, A]): ClientIO[A] =
      fromAttempt(result.leftMap(err => new Exception(err.show)))

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

    def raiseError[A](error: Throwable): ClientIO[A] =
      StateT.liftF(IO.raiseError(error))
  }

}
