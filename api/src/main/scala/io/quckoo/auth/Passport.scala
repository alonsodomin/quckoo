package io.quckoo.auth

import upickle.default._

import io.quckoo.serialization.Base64._

/**
  * Created by alonsodomin on 05/09/2016.
  */
object Passport {

  private final val SubjectClaim = "sub"

}

final class Passport(val token: String) {
  import Passport._

  private[this] val claims: Map[String, String] = {
    val jwtClaims = token.split('.')(1).toByteArray
    read[Map[String, String]](new String(jwtClaims, "UTF-8"))
  }

  override def equals(other: Any): Boolean = other match {
    case that: Passport => this.token == that.token
    case _              => false
  }

  override def hashCode: Int = token.hashCode

  lazy val principal: Option[Principal] =
    claims.get(SubjectClaim).map(User)

}
