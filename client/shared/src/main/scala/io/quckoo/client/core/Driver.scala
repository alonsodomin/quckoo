package io.quckoo.client.core

import io.quckoo.auth.{Credentials, Passport}

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Kleisli

/**
  * Created by alonsodomin on 08/09/2016.
  */
trait Driver[P <: Protocol] {
  type TransportRepr <: Transport[P]

  trait Encoding[In, Out] {
    val encode: Encoder[In, TransportRepr#Request]
    val decode: Decoder[TransportRepr#Response, Out]
  }

  trait Encodings {
    implicit val credentialsEnc: Encoding[Credentials, Passport]
  }

  protected val transport: TransportRepr
  val encodings: Encodings

  def invoke[In, Out](implicit
    ec: ExecutionContext,
    encoding: Encoding[In, Out]
  ): Kleisli[Future, Command[In], Out]

}
