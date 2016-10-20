package io.quckoo.serialization

import io.quckoo.util.Attempt

import scala.annotation.implicitNotFound

/**
  * Created by alonsodomin on 20/10/2016.
  */
@implicitNotFound("Can not decode ${In} into ${A} values")
trait Decoder[In, A] {
  def decode(input: In): Attempt[A]
}

object Decoder {
  @inline def apply[In, A](implicit ev: Decoder[In, A]): Decoder[In, A] = ev
}
