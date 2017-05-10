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

package io.quckoo.serialization.base64

import io.quckoo.serialization.Codec
import io.quckoo.util.Attempt

/**
  * Created by alonsodomin on 20/10/2016.
  */
final class Base64Codec(scheme: Scheme) extends Codec[String, Array[Byte]] {

  override def encode(a: String): Attempt[Array[Byte]] = {
    lazy val cleanS = a.reverse.dropWhile(_ == '=').reverse
    lazy val pad    = a.length - cleanS.length

    def threeBytes(s: String): Array[Byte] = {
      val r = s.map(scheme.decodeTable(_)).foldLeft(0)((a, b) => (a << 6) | b)
      Array((r >> 16).toByte, (r >> 8).toByte, r.toByte)
    }

    if (pad > 2 || a.length % 4 != 0) Attempt.fail {
      new java.lang.IllegalArgumentException("Invalid Base64 String:" + a)
    } else
      Attempt {
        (cleanS + "A" * pad).grouped(4).map(threeBytes).flatten.toArray.dropRight(pad)
      }
  }

  override def decode(input: Array[Byte]): Attempt[String] = {
    val pad = (3 - input.length % 3) % 3

    def sixBits(x: Array[Byte]): Array[Int] = {
      val a = (x(0) & 0xfc) >> 2
      val b = ((x(0) & 0x3) << 4) | ((x(1) & 0xf0) >> 4)
      val c = ((x(1) & 0xf) << 2) | ((x(2) & 0xc0) >> 6)
      val d = (x(2)) & 0x3f
      Array(a, b, c, d)
    }

    Attempt {
      ((input ++ zero.take(pad))
        .grouped(3)
        .flatMap(sixBits)
        .map(scheme.encodeTable)
        .toArray
        .dropRight(pad) :+ "=" * pad).mkString
    }
  }

}
