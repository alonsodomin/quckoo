package io.quckoo.auth

import io.quckoo.serialization.Base64._

/**
 * Created by alonsodomin on 14/10/2015.
 */
object XSRFToken {

  private final val TokenSeparator = ":"
  private[this] final val TokenPattern = s"(.+?)$TokenSeparator(.+)".r

  private final val ExpiredToken = "EXPIRED"

  def apply(encoded: String): XSRFToken = {
    val TokenPattern(token, userId) = new String(encoded.toByteArray, "UTF-8")
    XSRFToken(userId, token)
  }

}

case class XSRFToken(userId: UserId, private val value: String) {
  import XSRFToken._

  def expire(): XSRFToken =
    new XSRFToken(userId, ExpiredToken)

  def expired: Boolean = value == ExpiredToken

  override def toString: String =
    s"$value$TokenSeparator$userId".getBytes("UTF-8").toBase64

}
