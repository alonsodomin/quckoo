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

package io.quckoo.auth

import cats.{Eq, Show}
import cats.implicits._

import io.quckoo.serialization.DataBuffer
import io.quckoo.serialization.json._
import io.quckoo.util.Attempt

/**
  * Created by alonsodomin on 05/09/2016.
  */
object Passport {

  private final val SubjectClaim = "sub"

  def apply(token: String): Attempt[Passport] = {
    val tokenParts: Attempt[Array[String]] = {
      val parts = token.split('.')
      if (parts.length == 3) Right(parts)
      else Left(InvalidPassport(token))
    }

    def decodePart(part: Int): Attempt[DataBuffer] =
      tokenParts.map(_(part)).flatMap(DataBuffer.fromBase64)

    def parseClaims = decodePart(1).flatMap(_.as[Map[String, String]])

    parseClaims.map(new Passport(_, token))
  }

  implicit val passportEq: Eq[Passport] = Eq.fromUniversalEquals[Passport]

  implicit val passportShow: Show[Passport] = Show.fromToString

}

final class Passport(claims: Map[String, String], val token: String) {
  import Passport._

  lazy val principal: Option[Principal] =
    claims.get(SubjectClaim).map(User)

  override def equals(other: Any): Boolean = other match {
    case that: Passport => this.token === that.token
    case _              => false
  }

  override def hashCode(): Int = token.hashCode

  override def toString: String = token

}
