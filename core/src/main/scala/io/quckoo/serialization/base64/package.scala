package io.quckoo.serialization

/**
  * Created by alonsodomin on 20/10/2016.
  */
package object base64 {
  private[base64] val zero = Array(0, 0).map(_.toByte)

  lazy val DefaultScheme = new Scheme(
    ('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9') ++ Seq('+', '/'))
  lazy val UrlScheme = new Scheme(DefaultScheme.encodeTable.dropRight(2) ++ Seq('-', '_'))

  implicit val Base64Codec = new Base64Codec(DefaultScheme)
}
