package io.quckoo.serialization

/**
  * Created by alonsodomin on 20/10/2016.
  */
trait Codec[A, Ser] extends Encoder[A, Ser] with Decoder[Ser, A]
