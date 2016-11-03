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

import io.quckoo.client.core.Protocol
import io.quckoo.client.free.driver.Driver
import io.quckoo.client.free.marshalling.Marshaller
import io.quckoo.util._

import scalaz._
import Scalaz._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by domingueza on 03/11/2016.
  */
object interpreter {

  trait Interpreter[Op[_]] {
    type FreeOp[A] = Free[Op, A]

    def interp[P <: Protocol](implicit marshaller: Marshaller[P], ec: ExecutionContext): Op ~> Kleisli[Future, Driver[P], ?] = new (Op ~> Kleisli[Future, Driver[P], ?]) {
      def apply[A](op: Op[A]): Kleisli[Future, Driver[P], A] = Kleisli { driver =>
        val marshall = marshaller.marshall[Op[A]].transform(attempt2Future)
        val unmarshall = marshaller.unmarshall[A].transform(attempt2Future)

        (marshall >=> driver.send >=> unmarshall).run(op)
      }
    }

  }

}
