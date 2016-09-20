package io.quckoo.serialization

import upickle.default.{Reader => UReader, Writer => UWriter}
import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}

import io.quckoo.serialization.json.{JsonReader, JsonWriter}
import io.quckoo.util.LawfulTry

import scala.language.implicitConversions
import scalaz._

/**
  * Created by alonsodomin on 15/09/2016.
  */
final class DataBuffer private (protected val buffer: ByteBuffer) extends AnyVal {
  import Base64._

  def isEmpty: Boolean = buffer.remaining() == 0

  def +(that: DataBuffer): DataBuffer = {
    val newBuffer = ByteBuffer.allocateDirect(
      buffer.remaining() + that.buffer.remaining()
    )
    newBuffer.put(buffer)
    newBuffer.put(that.buffer)

    buffer.rewind()
    that.buffer.rewind()
    newBuffer.rewind()

    new DataBuffer(newBuffer)
  }

  def as[A: UReader]: LawfulTry[A] = JsonReader[A].run(asString())

  def asString(charset: Charset = StandardCharsets.UTF_8): String = {
    val content = charset.decode(buffer).toString
    buffer.rewind()
    content
  }

  def toBase64: String = {
    val bytes = new Array[Byte](buffer.remaining())
    buffer.get(bytes, buffer.position(), buffer.remaining())
    buffer.rewind()
    bytes.toBase64
  }

  def toByteBuffer: ByteBuffer =
    buffer.asReadOnlyBuffer()

}

object DataBuffer {
  import Base64._

  final val Empty = new DataBuffer(ByteBuffer.allocateDirect(0))

  def apply[A: UWriter](a: A, charset: Charset = StandardCharsets.UTF_8): LawfulTry[DataBuffer] =
    JsonWriter[A].map(str => fromString(str, charset)).run(a)

  def apply(buffer: ByteBuffer): DataBuffer =
    new DataBuffer(buffer.asReadOnlyBuffer())

  def apply(bytes: Array[Byte]): DataBuffer =
    apply(ByteBuffer.wrap(bytes))

  def fromString(str: String, charset: Charset = StandardCharsets.UTF_8): DataBuffer =
    apply(str.getBytes(charset))

  def fromBase64(str: String): DataBuffer =
    apply(str.toByteArray)

  implicit val dataBufferInstance = new Monoid[DataBuffer] {
    override def append(f1: DataBuffer, f2: => DataBuffer): DataBuffer = f1 + f2
    override def zero: DataBuffer = Empty
  }

}
