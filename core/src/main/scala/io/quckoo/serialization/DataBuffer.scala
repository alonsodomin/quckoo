/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.serialization

import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}

import cats.Monoid

import io.quckoo.serialization.base64._
import io.quckoo.util.Attempt

/**
  * Created by alonsodomin on 15/09/2016.
  */
final class DataBuffer private (protected val buffer: ByteBuffer) extends AnyVal {

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

  def as[A](implicit decoder: Decoder[String, A]): Attempt[A] =
    decoder.decode(asString())

  def asString(charset: Charset = StandardCharsets.UTF_8): String = {
    val content = charset.decode(buffer).toString
    buffer.rewind()
    content
  }

  def toArray: Array[Byte] = {
    val bytes = new Array[Byte](buffer.remaining())
    buffer.get(bytes, buffer.position(), buffer.remaining())
    buffer.rewind()
    bytes
  }

  def toBase64: Attempt[String] =
    Base64Codec.decode(toArray)

  def toByteBuffer: ByteBuffer =
    buffer.asReadOnlyBuffer()

}

object DataBuffer {

  final val Empty = new DataBuffer(ByteBuffer.allocateDirect(0))

  def apply[A](a: A, charset: Charset = StandardCharsets.UTF_8)(
      implicit encoder: Encoder[A, String]): Attempt[DataBuffer] =
    encoder.encode(a).map(str => fromString(str, charset))

  def apply(buffer: ByteBuffer): DataBuffer =
    new DataBuffer(buffer.asReadOnlyBuffer())

  def apply(bytes: Array[Byte]): DataBuffer =
    apply(ByteBuffer.wrap(bytes))

  def fromString(str: String, charset: Charset = StandardCharsets.UTF_8): DataBuffer =
    apply(str.getBytes(charset))

  def fromBase64(str: String): Attempt[DataBuffer] =
    Base64Codec.encode(str).map(apply)

  implicit val dataBufferInstance = new Monoid[DataBuffer] {
    def combine(f1: DataBuffer, f2: DataBuffer): DataBuffer = f1 + f2
    def empty: DataBuffer                                   = Empty
  }

}
