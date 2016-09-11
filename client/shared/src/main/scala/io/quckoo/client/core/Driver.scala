package io.quckoo.client.core

import io.quckoo.auth.{Credentials, Passport}

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Kleisli

/**
  * Created by alonsodomin on 08/09/2016.
  */
trait Driver[P <: Protocol] {
  type TransportRepr <: Transport[P]

  trait Marshalling[In, Out] {
    val to: Marshall[In, TransportRepr#Request]
    val from: Unmarshall[TransportRepr#Response, Out]
  }

  trait Marshallers {
    implicit val authMarshaller: Marshalling[Credentials, Passport]
  }

  protected val transport: TransportRepr
  val api: Marshallers

  def invoke[In, Out](implicit
    ec: ExecutionContext,
    marshalling: Marshalling[In, Out]
  ): Kleisli[Future, Command[In], Out]

}
