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

import cats.data.StateT
import cats.effect.{Sync, Async}

import scala.concurrent.Future

/**
  * Created by alonsodomin on 03/06/2017.
  */
package object shell {

  type ShellOp[F[_], A] = StateT[F, ShellContext, A]
  object ShellOp {
    def unit[F[_]](implicit F: Sync[F]): ShellOp[F, Unit] =
      StateT.pure(())

    def lift[F[_], A](a: F[A])(implicit F: Sync[F]): ShellOp[F, A] =
      StateT.lift(a)

    def delay[F[_], A](a: => A)(implicit F: Sync[F]): ShellOp[F, A] =
      StateT(ctx => F.map(F.delay(a))(ctx -> _))

    def suspend[F[_], A](op: => ShellOp[F, A])(implicit F: Async[F]): ShellOp[F, A] =
      StateT(ctx => F.suspend(op.run(ctx)))
  }

  implicit class ShellOpExt[F[_], A](val self: ShellOp[F, A]) {
    def attempt(implicit F: Sync[F]): ShellOp[F, Either[Throwable, A]] = StateT { ctx =>
      F.map(F.attempt(self.run(ctx))) {
        case Right((_, x)) => ctx -> Right(x)
        case Left(err)     => ctx -> Left(err)
      }
    }
  }

  type ClientOp[A] = ShellOp[Future, A]

}
