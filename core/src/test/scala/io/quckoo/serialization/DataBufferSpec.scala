package io.quckoo.serialization

import java.nio.charset.StandardCharsets

import io.quckoo.serialization.base64.Base64Codec
import io.quckoo.serialization.json._

import org.scalatest.{EitherValues, FlatSpec, Matchers}

/**
  * Created by alonsodomin on 16/09/2016.
  */
class DataBufferSpec extends FlatSpec with EitherValues with Matchers {

  def emptyBuffer = DataBuffer.Empty

  val sampleCharsets = List(StandardCharsets.ISO_8859_1, StandardCharsets.US_ASCII, StandardCharsets.UTF_8,
    StandardCharsets.UTF_16, StandardCharsets.UTF_16BE, StandardCharsets.UTF_16LE)

  "A DataBuffer (when empty)" should "be empty" in {
    emptyBuffer.isEmpty shouldBe true
  }

  "A DataBuffer (non empty)" should "deserialize to the original serialized object" in {
    val data = Some(10)
    val returnedBackNForth = DataBuffer(data).flatMap(_.as[Option[Int]]).toEither

    returnedBackNForth.right.value shouldBe data
  }

  it should "concatenate buffers" in {
    val (left, right) = {
      val (hello, world) = "Hello World!".splitAt(6)
      (hello.getBytes(StandardCharsets.UTF_8), world.getBytes(StandardCharsets.UTF_8))
    }

    val concatenated = DataBuffer(left) + DataBuffer(right)

    concatenated.asString() shouldBe "Hello World!"
  }

  it should "decode strings in any standard charset" in {
    for (charset <- sampleCharsets) {
      DataBuffer.fromString("foo", charset).asString(charset) shouldBe "foo"
    }
  }

  it should "decode strings in Base64" in {

    for (charset <- sampleCharsets) {
      val result = Base64Codec.decode("foo".getBytes(charset))
        .flatMap(DataBuffer.fromBase64)
        .map(_.asString(charset))
        .toEither

      result.right.value shouldBe "foo"
    }
  }

  it should "encode anything is Base64" in {

    for (charset <- sampleCharsets) {
      val result = DataBuffer.fromString("banana", charset)
        .toBase64
        .flatMap(Base64Codec.encode)
        .toEither

      val expected = "banana".getBytes(charset)
      result.right.value shouldBe expected
    }
  }

}
