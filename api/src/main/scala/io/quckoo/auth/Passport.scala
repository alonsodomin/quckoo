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

import io.quckoo.serialization.{Base64, DataBuffer}
import io.quckoo.util.{LawfulTry, lawfulTry2Try}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 05/09/2016.
  */
object Passport {

  private final val SubjectClaim = "sub"

  def apply(token: String): LawfulTry[Passport] = {
    import Base64._

    val tokenParts: LawfulTry[Array[String]] = {
      val parts = token.split('.')
      if (parts.length == 3) parts.right[InvalidPassportException]
      else InvalidPassportException(token).left[Array[String]]
    }

    def decodePart(part: Int): LawfulTry[DataBuffer] =
      tokenParts.map(_(part)).flatMap(str => LawfulTry(str.toByteArray)).map(DataBuffer.apply)

    def parseClaims = decodePart(1).flatMap(_.as[Map[String, String]])

    parseClaims.map(claims => new Passport(claims, token))
  }

  implicit val instance = Equal.equalA[Passport]

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
