package io.quckoo.client.http

import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}

import upickle.default._

import scala.util.Try

/**
  * Created by alonsodomin on 15/09/2016.
  */
final class HttpEntity private (val buffer: ByteBuffer) extends AnyVal {

  def as[A: Reader]: Try[A] = asString().flatMap(str => Try(read[A](str)))

  def asString(charset: Charset = StandardCharsets.UTF_8): Try[String] =
    Try(charset.decode(buffer).toString)

}

object HttpEntity {
  def apply[A: Writer](a: A, charset: Charset = StandardCharsets.UTF_8): Try[HttpEntity] = Try {
    val bytes = write(a).getBytes(charset)
    new HttpEntity(ByteBuffer.wrap(bytes))
  }

  def apply(buffer: ByteBuffer): HttpEntity =
    new HttpEntity(buffer)
}
