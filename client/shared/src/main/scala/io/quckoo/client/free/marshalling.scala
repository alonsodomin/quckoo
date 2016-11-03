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
