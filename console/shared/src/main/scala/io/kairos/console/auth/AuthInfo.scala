package io.kairos.console.auth

import io.kairos.serialization.Base64._

/**
 * Created by alonsodomin on 14/10/2015.
 */
object AuthInfo {

  private final val TokenSeparator = ":"
  private[this] final val TokenPattern = s"(.+?)$TokenSeparator(.+)".r

  private final val ExpiredToken = "EXPIRED"

  def apply(encoded: String): AuthInfo = {
    val TokenPattern(userId, token) = new String(encoded.toByteArray, "UTF-8")
    AuthInfo(userId, token)
  }

}

case class AuthInfo(userId: UserId, token: String) {
  import AuthInfo._

  def expire(): AuthInfo =
    new AuthInfo(userId, ExpiredToken)

  override def toString: String =
    s"$userId$TokenSeparator$token".getBytes("UTF-8").toBase64

}
