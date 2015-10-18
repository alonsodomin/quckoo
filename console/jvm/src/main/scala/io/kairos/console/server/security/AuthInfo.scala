package io.kairos.console.server.security

import java.util.UUID

import io.kairos.console.auth.UserId
import io.kairos.console.base64.Base64._

/**
 * Created by alonsodomin on 14/10/2015.
 */
object AuthInfo {

  private[this] val TokenPattern = "(.+?):(.+)".r

  private val ExpiredToken = "EXPIRED"

  def apply(encoded: String): AuthInfo = {
    val TokenPattern(userId, token) = new String(encoded.toByteArray, "UTF-8")
    new AuthInfo(userId, token)
  }

  private def newToken(): String = UUID.randomUUID().toString

}

class AuthInfo private (val userId: UserId, val token: String) {
  import AuthInfo._

  def this(userId: String) = this(userId, AuthInfo.newToken())

  def hasPermission(permission: Permission): Boolean =
    if (ExpiredToken == token) false
    else ???

  def refresh(): AuthInfo =
    new AuthInfo(userId, newToken())

  def expire(): AuthInfo =
    new AuthInfo(userId, ExpiredToken)

  override def toString: String =
    (userId + ":" + token).getBytes("UTF-8").toBase64

}
