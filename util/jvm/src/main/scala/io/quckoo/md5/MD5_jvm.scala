package io.quckoo.md5

import java.security.MessageDigest
import java.nio.charset.StandardCharsets

object MD5 {

  // $COVERAGE-OFF$
  def checksum(input: String): String = {
    val md = MessageDigest.getInstance("MD5")
    md.update(input.getBytes(StandardCharsets.UTF_8))

    val digest = md.digest()
    val builder = new StringBuilder()
    digest.foreach { byte =>
      builder.append(f"${byte & 0xff}%02x")
    }
    builder.toString
  }
  // $COVERAGE-ON$

}
