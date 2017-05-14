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
