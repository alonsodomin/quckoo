package io.quckoo.serialization

import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}

import io.quckoo.serialization.json.{JsonReaderT, JsonWriterT}
import io.quckoo.util.TryE

import upickle.default.{Reader => UReader, Writer => UWriter}

import scala.language.implicitConversions

import scalaz._

/**
  * Created by alonsodomin on 15/09/2016.
  */
final class DataBuffer private (protected val buffer: ByteBuffer) extends AnyVal {
  import Base64._

  def +(that: DataBuffer): DataBuffer = {
    val newBuffer = ByteBuffer.allocateDirect(
      buffer.remaining() + that.buffer.remaining()
    )
    newBuffer.put(buffer)
    newBuffer.put(that.buffer)
    new DataBuffer(newBuffer)
  }

  def as[A: UReader]: TryE[A] = JsonReaderT[A].run(asString())

  def asString(charset: Charset = StandardCharsets.UTF_8): String =
    charset.decode(buffer).toString

  def toBase64: String = {
    val bytes = new Array[Byte](buffer.remaining())
    buffer.get(bytes, buffer.position(), buffer.remaining())
    bytes.toBase64
  }

}

object DataBuffer {

  def apply[A: UWriter](a: A, charset: Charset = StandardCharsets.UTF_8): TryE[DataBuffer] =
    JsonWriterT[A].map(str => fromString(str, charset)).run(a)

  def apply(buffer: ByteBuffer): DataBuffer =
    new DataBuffer(buffer)

  def apply(bytes: Array[Byte]): DataBuffer =
    apply(ByteBuffer.wrap(bytes))

  def fromString(str: String, charset: Charset = StandardCharsets.UTF_8): DataBuffer =
    new DataBuffer(ByteBuffer.wrap(str.getBytes(charset)))

  implicit val dataBufferInstance = new Semigroup[DataBuffer] {
    override def append(f1: DataBuffer, f2: => DataBuffer): DataBuffer = f1 + f2
  }

  implicit def toByteBuffer(dataBuffer: DataBuffer): ByteBuffer =
    dataBuffer.buffer.asReadOnlyBuffer()

}
