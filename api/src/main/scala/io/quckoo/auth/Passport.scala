package io.quckoo.auth

import io.quckoo.serialization.{Base64, DataBuffer}
import io.quckoo.util.{TryE, either2Try}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 05/09/2016.
  */
object Passport {

  private final val SubjectClaim = "sub"

  def apply(token: String): TryE[Passport] = {
    import Base64._

    val tokenParts: TryE[Array[String]] = {
      val parts = token.split('.')
      if (parts.length == 3) parts.right[InvalidPassportException]
      else InvalidPassportException(token).left[Array[String]]
    }

    def decodePart(part: Int): TryE[DataBuffer] =
      tokenParts.map(_(part)).flatMap(str => TryE(str.toByteArray)).map(DataBuffer.apply)

    def parseHeader = decodePart(0).flatMap(_.as[Map[String, String]])
    def parseClaims = decodePart(1).flatMap(_.as[Map[String, String]])
    def parseSig    = decodePart(2)

    for {
      header <- parseHeader
      claims <- parseClaims
      sig    <- parseSig
    } yield new Passport(header, claims, sig)
  }

  implicit val instance = Equal.equalA[Passport]

}

final class Passport(header: Map[String, String], claims: Map[String, String], signature: DataBuffer) {
  import Passport._

  lazy val token: String = {
    val tk = for {
      hdr <- DataBuffer(header).map(_.toBase64)
      cls <- DataBuffer(claims).map(_.toBase64)
    } yield s"$hdr.$cls.${signature.toBase64}"

    either2Try(tk).get
  }

  lazy val principal: Option[Principal] =
    claims.get(SubjectClaim).map(User)

  override def equals(other: Any): Boolean = other match {
    case that: Passport => this.token === that.token
    case _              => false
  }

  override def hashCode(): Int = token.hashCode

  override def toString: String = token

}
