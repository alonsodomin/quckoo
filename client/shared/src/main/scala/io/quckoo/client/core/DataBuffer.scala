package io.quckoo.client.core

import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}

import upickle.default._

import scala.util.Try
import scala.language.implicitConversions

import scalaz.Semigroup

/**
  * Created by alonsodomin on 15/09/2016.
  */
final class DataBuffer private (protected val buffer: ByteBuffer) extends AnyVal {

  def +(that: DataBuffer): DataBuffer = {
    val newBuffer = ByteBuffer.allocateDirect(buffer.remaining() + that.buffer.remaining())
    newBuffer.put(buffer)
    newBuffer.put(that.buffer)
    new DataBuffer(newBuffer)
  }

  def as[A: Reader]: Try[A] = asString().flatMap(str => Try(read[A](str)))

  def asString(charset: Charset = StandardCharsets.UTF_8): Try[String] =
    Try(charset.decode(buffer).toString)

}

object DataBuffer {

  def apply[A: Writer](a: A, charset: Charset = StandardCharsets.UTF_8): Try[DataBuffer] = Try {
    val bytes = write(a).getBytes(charset)
    new DataBuffer(ByteBuffer.wrap(bytes))
  }

  def apply(buffer: ByteBuffer): DataBuffer =
    new DataBuffer(buffer)

  implicit val dataBufferInstance = new Semigroup[DataBuffer] {
    override def append(f1: DataBuffer, f2: => DataBuffer): DataBuffer = f1 + f2
  }

  implicit def toByteBuffer(dataBuffer: DataBuffer): ByteBuffer =
    dataBuffer.buffer.asReadOnlyBuffer()

}
