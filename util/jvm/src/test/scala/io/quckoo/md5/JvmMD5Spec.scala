package io.quckoo.md5

import org.scalatest._

class JvmMD5Spec extends FlatSpec with Matchers {

  "MD5" should "generate MD5 checksums" in {
    val givenInput       = "foo"
    val expectedChecksum = "acbd18db4cc2f85cedef654fccc4a4d8"

    val returnedChecksum = MD5.checksum(givenInput)

    returnedChecksum shouldBe expectedChecksum
  }

}
