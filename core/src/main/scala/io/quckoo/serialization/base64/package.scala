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

/**
  * Created by alonsodomin on 20/10/2016.
  */
package object base64 {
  private[base64] val zero = Array(0, 0).map(_.toByte)

  lazy val DefaultScheme = new Scheme(
    ('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9') ++ Seq('+', '/'))
  lazy val UrlScheme = new Scheme(DefaultScheme.encodeTable.dropRight(2) ++ Seq('-', '_'))

  implicit val Base64Codec = new Base64Codec(DefaultScheme)
}
