package io.quckoo.serialization

import io.quckoo.util.Attempt

import scala.annotation.implicitNotFound

/**
  * Created by alonsodomin on 20/10/2016.
  */
@implicitNotFound("Can not encode ${A} values into ${Out}")
trait Encoder[A, Out] {
  def encode(a: A): Attempt[Out]
}

object Encoder {
  @inline def apply[A, Out](implicit ev: Encoder[A, Out]): Encoder[A, Out] = ev
}
