package io.quckoo.md5

import scala.scalajs.js.typedarray._

object MD5 {

  // $COVERAGE-OFF$
  def checksum(input: String): String = {
    val buffer = new SparkMD5.ArrayBuffer()
    buffer.append(input.getBytes("UTF-8").toTypedArray.buffer)
    buffer.end()
  }
  // $COVERAGE-ON$

}
