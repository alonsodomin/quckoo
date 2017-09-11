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
import cats.effect.IO

import io.quckoo.auth.{NotAuthorized, Session}

package object api2 {

  type QuckooIO[A] = StateT[IO, Session, A]
  object QuckooIO {
    @inline
    def apply[A](f: Session => IO[A]): QuckooIO[A] =
      StateT.inspectF(f)

    def auth[A](f: Session.Authenticated => IO[A]): QuckooIO[A] = QuckooIO {
      case auth: Session.Authenticated => f(auth)
      case _ => IO.raiseError(NotAuthorized)
    }

    @inline
    def session(f: Session => IO[Session]): QuckooIO[Unit] =
      StateT.modifyF(f)

    def suspend[A](io: => QuckooIO[A]): QuckooIO[A] = StateT { session =>
      IO.suspend(io.run(session))
    }
  }
  
}
