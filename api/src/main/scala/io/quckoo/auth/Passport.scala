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
