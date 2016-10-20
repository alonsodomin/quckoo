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

package io.quckoo

import scala.concurrent.Future
import scala.util.{Try, Success => StdSuccess, Failure => StdFailure}

import scalaz._
import Scalaz._
import Isomorphism._

/**
  * Created by alonsodomin on 15/09/2016.
  */
package object util {

  type Attempt[+A] = Throwable \/ A
  object Attempt {
    @inline def apply[A](thunk: => A): Attempt[A] =
      \/.fromTryCatchNonFatal(thunk)

    @inline def unit: Attempt[Unit]                = \/.right[Throwable, Unit](())
    @inline def success[A](a: A): Attempt[A]       = \/.right[Throwable, A](a)
    @inline def fail[A](ex: Throwable): Attempt[A] = \/.left[Throwable, A](ex)
  }

  final val attempt2Try = new (Attempt ~> Try) {
    override def apply[A](fa: Attempt[A]): Try[A] = fa match {
      case -\/(throwable) => StdFailure(throwable)
      case \/-(value)     => StdSuccess(value)
    }
  }

  final val try2Attempt = new (Try ~> Attempt) {
    override def apply[A](fa: Try[A]): Attempt[A] = fa match {
      case StdSuccess(value) => value.right[Throwable]
      case StdFailure(ex)    => ex.left[A]
    }
  }

  final val attemptIso = new (Attempt <~> Try) {
    override def to: ~>[Attempt, Try]   = attempt2Try
    override def from: ~>[Try, Attempt] = try2Attempt
  }

  final val try2Future = new (Try ~> Future) {
    override def apply[A](fa: Try[A]): Future[A] =
      Future.fromTry(fa)
  }

  final val attempt2Future: Attempt ~> Future = attempt2Try andThen try2Future

}
