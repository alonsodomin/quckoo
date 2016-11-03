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
import io.quckoo.client.free.capture.Capture
import io.quckoo.client.http.{HttpRequest, HttpResponse}
import io.quckoo.util.Attempt

import scalaz.{Kleisli, Monad}

/**
  * Created by domingueza on 03/11/2016.
  */
object marshalling {

  trait Marshaller[P <: Protocol] {
    def marshall[A]: Kleisli[Attempt, A, P#Request]
    def unmarshall[A]: Kleisli[Attempt, P#Response, A]
  }

  /*type Marshaller[P <: Protocol, A] = Kleisli[Attempt, A, P#Request]
  type Unmarshaller[P <: Protocol, A] = Kleisli[Attempt, P#Response, A]

  trait HttpProto extends Protocol {
    type Request = HttpRequest
    type Response = HttpResponse
  }

  type HttpMarshaller[A] = Marshaller[HttpProto, A]
  type HttpUnmarshaller[A] = Unmarshaller[HttpProto, A]*/

  /*object Marshaller {
    def apply[M[_] : Monad : Capture, In, Out](f: In => Out): Marshaller[M, In, Out] =
      Kleisli((input: In) => Capture[M].apply(f(input)))
  }*/

}
