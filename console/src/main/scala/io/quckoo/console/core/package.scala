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

package io.quckoo.console

import cats.data.Kleisli
import cats.effect.{IO, LiftIO}
import cats.implicits._

import diode.{ActionType, Effect}

import io.quckoo.auth.PassportExpired
import io.quckoo.client.{ClientIO, QuckooClient2}

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

/**
  * Created by alonsodomin on 05/07/2016.
  */
package object core {

  type ConsoleIO[A] = Kleisli[ClientIO, QuckooClient2, A]
  object ConsoleIO {

    @inline
    def apply[A](f: QuckooClient2 => ClientIO[A]): ConsoleIO[A] =
      refreshingToken(Kleisli(f))

    private def refreshingToken[A](action: ConsoleIO[A]): ConsoleIO[A] = Kleisli { client =>
      val handleExpired = ClientIO.suspend(client.refreshToken() >> action.run(client))

      action.run(client).recoverWith {
        case PassportExpired(_) => handleExpired
      }
    }

    def local[A](action: IO[A]): ConsoleIO[A] = Kleisli { _ =>
      LiftIO[ClientIO].liftIO(action)
    }

  }

  implicit def io2Effect[A: ActionType](io: IO[A])(implicit ec: ExecutionContext): Effect =
    Effect(io.unsafeToFuture())

  implicit def action2Effect[A: ActionType](action: => A)(implicit ec: ExecutionContext): Effect =
    Effect.action[A](action)

}
