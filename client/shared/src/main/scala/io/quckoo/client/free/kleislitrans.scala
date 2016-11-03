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

package io.quckoo.client.free

import scalaz._

/**
  * Created by domingueza on 03/11/2016.
  *
  * Stolen from Doobie:
  * https://github.com/tpolecat/doobie/blob/series/0.3.x/yax/core/src/main/scala/doobie/free/kleislitrans.scala
  */
object kleislitrans {

  trait KleisliTrans[Op[_]] {
    type Carrier

    type FreeOp[A] = Free[Op, A]

    def interpK[M[_]: Monad]: Op ~> Kleisli[M, Carrier, ?]
    def transK[M[_]: Monad]: FreeOp ~> Kleisli[M, Carrier, ?] = new (FreeOp ~> Kleisli[M, Carrier, ?]) {
      def apply[A](ma: FreeOp[A]): Kleisli[M, Carrier, A] =
        ma.foldMap[Kleisli[M, Carrier, ?]](interpK[M])
    }

    def trans[M[_]: Monad](c: Carrier): FreeOp ~> M = new (FreeOp ~> M) {
      def apply[A](ma: FreeOp[A]): M[A] =
        transK[M].apply[A](ma).run(c)
    }
  }

  object KleisliTrans {
    type Aux[O[_], C0] = KleisliTrans[O] { type Carrier = C0 }
  }

}
