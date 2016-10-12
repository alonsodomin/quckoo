/*
 * Copyright 2016 Antonio Alonso Dominguez
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

import scala.collection.immutable.HashMap

/**
  * Created by alonsodomin on 14/10/2015.
  */
object Base64 {

  private[this] val zero = Array(0, 0).map(_.toByte)
  class B64Scheme(val encodeTable: IndexedSeq[Char]) {
    lazy val decodeTable = HashMap(encodeTable.zipWithIndex: _*)
  }

  lazy val base64    = new B64Scheme(('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9') ++ Seq('+', '/'))
  lazy val base64Url = new B64Scheme(base64.encodeTable.dropRight(2) ++ Seq('-', '_'))

  implicit class Encoder(b: Array[Byte]) {

    lazy val pad = (3 - b.length % 3) % 3

    def toBase64(implicit scheme: B64Scheme = base64): String = {
      def sixBits(x: Array[Byte]): Array[Int] = {
        val a = (x(0) & 0xfc) >> 2
        val b = ((x(0) & 0x3) << 4) | ((x(1) & 0xf0) >> 4)
        val c = ((x(1) & 0xf) << 2) | ((x(2) & 0xc0) >> 6)
        val d = (x(2)) & 0x3f
        Array(a, b, c, d)
      }
      ((b ++ zero.take(pad))
        .grouped(3)
        .flatMap(sixBits)
        .map(scheme.encodeTable)
        .toArray
        .dropRight(pad) :+ "=" * pad).mkString
    }
  }

  implicit class Decoder(s: String) {
    lazy val cleanS = s.reverse.dropWhile(_ == '=').reverse
    lazy val pad    = s.length - cleanS.length

    def toByteArray(implicit scheme: B64Scheme = base64): Array[Byte] = {
      def threeBytes(s: String): Array[Byte] = {
        val r = s.map(scheme.decodeTable(_)).foldLeft(0)((a, b) => (a << 6) | b)
        Array((r >> 16).toByte, (r >> 8).toByte, r.toByte)
      }
      if (pad > 2 || s.length % 4 != 0)
        throw new java.lang.IllegalArgumentException("Invalid Base64 String:" + s)
      try {
        (cleanS + "A" * pad).grouped(4).map(threeBytes).flatten.toArray.dropRight(pad)
      } catch {
        case e: NoSuchElementException =>
          throw new java.lang.IllegalArgumentException("Invalid Base64 String:" + s)
      }
    }
  }

}
