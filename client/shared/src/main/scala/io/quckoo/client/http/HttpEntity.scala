package io.quckoo.client.http

import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}

import upickle.default._

/**
  * Created by alonsodomin on 15/09/2016.
  */
final class HttpEntity(val buffer: ByteBuffer) extends AnyVal {

  def as[A: Reader]: A = read[A](asString())

  def asString(charset: Charset = StandardCharsets.UTF_8): String =
    charset.decode(buffer).toString

}

object HttpEntity {
  def apply[A: Writer](a: A, charset: Charset = StandardCharsets.UTF_8): HttpEntity = {
    val bytes = write(a).getBytes(charset)
    new HttpEntity(ByteBuffer.wrap(bytes))
  }
}
