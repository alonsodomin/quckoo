package io.quckoo.client.free

import io.quckoo.serialization.{Decoder, Encoder}

/**
  * Created by domingueza on 03/11/2016.
  */
object marshalling {

  sealed trait MarshallerOp[In, Out]
  object MarshallerOp {
    case class MarshallOp[In, Out](input: In, encoder: Encoder[In, Out]) extends MarshallerOp[In, Out]
    case class UnmarshallOp[In, Out](input: In, decoder: Decoder[In, Out]) extends MarshallerOp[In, Out]
  }


}
