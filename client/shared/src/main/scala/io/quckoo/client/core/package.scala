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

package io.quckoo.client

import cats.~>
import cats.data.Kleisli

import io.quckoo.util.Attempt

import monix.reactive.Observable

/**
  * Created by alonsodomin on 10/09/2016.
  */
package object core {

  type Marshall[Cmd[_] <: Command[_], In, Req] = Kleisli[Attempt, Cmd[In], Req]
  object Marshall {
    @inline
    def apply[Cmd[_] <: Command[_], In, Req](
        run: Cmd[In] => Attempt[Req]): Marshall[Cmd, In, Req] =
      Kleisli[Attempt, Cmd[In], Req](run)
  }

  type Unmarshall[Res, Rslt] = Kleisli[Attempt, Res, Rslt]
  object Unmarshall {
    @inline def apply[Res, Rslt](run: Res => Attempt[Rslt]): Unmarshall[Res, Rslt] =
      Kleisli[Attempt, Res, Rslt](run)
  }

  final val attempt2Observable = new (Attempt ~> Observable) {
    override def apply[A](fa: Attempt[A]): Observable[A] = fa match {
      case Right(value) => Observable.eval(value)
      case Left(ex)     => Observable.raiseError(ex)
    }
  }

}
