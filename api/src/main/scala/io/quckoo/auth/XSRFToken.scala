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

package io.quckoo.auth

import io.quckoo.serialization.Base64._

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 14/10/2015.
  */
object XSRFToken {

  private final val TokenSeparator     = ":"
  private[this] final val TokenPattern = s"(.+?)$TokenSeparator(.+)".r

  private final val ExpiredToken = "EXPIRED"

  def apply(encoded: String): XSRFToken = {
    val TokenPattern(token, userId) = new String(encoded.toByteArray, "UTF-8")
    XSRFToken(userId, token)
  }

  implicit val equality: Equal[XSRFToken] = Equal.equalBy(_.toString)

}

final case class XSRFToken(userId: UserId, private val value: String) {
  import XSRFToken._

  def expire(): XSRFToken =
    new XSRFToken(userId, ExpiredToken)

  def expired: Boolean = value === ExpiredToken

  override def toString: String =
    s"$value$TokenSeparator$userId".getBytes("UTF-8").toBase64

}
