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
import cats.data.{EitherT, Validated}
import cats.effect.IO
import cats.implicits._

import scala.concurrent.Future
import scala.util._

/**
  * Created by alonsodomin on 15/09/2016.
  */
package object util {

  type Attempt[+A] = Either[Throwable, A]
  object Attempt {
    @inline def apply[A](thunk: => A): Attempt[A] =
      Either.catchNonFatal(thunk)

    @inline def unit: Attempt[Unit]                = Either.right[Throwable, Unit](())
    @inline def success[A](a: A): Attempt[A]       = Either.right[Throwable, A](a)
    @inline def fail[A](ex: Throwable): Attempt[A] = Either.left[Throwable, A](ex)
  }

  implicit final val attempt2Try: Attempt ~> Try = new (Attempt ~> Try) {
    override def apply[A](fa: Attempt[A]): Try[A] = fa match {
      case Left(throwable) => Failure(throwable)
      case Right(value)    => Success(value)
    }
  }

  implicit final val try2Attempt: Try ~> Attempt = new (Try ~> Attempt) {
    override def apply[A](fa: Try[A]): Attempt[A] = fa match {
      case Success(value) => Right(value)
      case Failure(ex)    => Left(ex)
    }
  }

  implicit final val try2Future: Try ~> Future = new (Try ~> Future) {
    override def apply[A](fa: Try[A]): Future[A] =
      Future.fromTry(fa)
  }

  implicit final val attempt2IO: Attempt ~> IO = new (Attempt ~> IO) {
    override def apply[A](fa: Attempt[A]): IO[A] =
      IO.async(_(fa))
  }

  implicit final val attempt2Future: Attempt ~> Future =
    attempt2Try andThen try2Future

  implicit class RichValidated[E, A](val self: Validated[E, A]) extends AnyVal {
    def append[EE >: E, AA >: A](other: Validated[EE, AA])(implicit es: Semigroup[EE], as: Semigroup[AA]): Validated[EE, AA] = {
      import Validated._

      (self, other) match {
        case (Valid(a1), Valid(a2))     => Valid(as.combine(a1, a2))
        case (Valid(_), Invalid(_))     => self
        case (Invalid(_), Valid(_))     => other
        case (Invalid(e1), Invalid(e2)) => Invalid(es.combine(e1, e2))
      }
    }
  }

  implicit class RichOptionEitherT[E, A](val self: EitherT[Option, E, A]) extends AnyVal {
    def cozip: Either[Option[E], Option[A]] =
      self.value.fold(none[E].asLeft[Option[A]])(_.bimap(Some(_), Some(_)))
  }

}
