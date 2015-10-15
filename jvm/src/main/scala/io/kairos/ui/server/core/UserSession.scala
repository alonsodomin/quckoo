package io.kairos.ui.server.core

import java.util.UUID

import akka.actor.{Actor, Props}
import io.kairos.ui.server.security.AuthInfo

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * Created by alonsodomin on 14/10/2015.
 */
object UserSession {

  type SessionId = UUID

  case class ValidateToken(token: String)
  case class AuthToken(token: String)
  case object InvalidToken

  def props(sessionId: SessionId, authInfo: AuthInfo, timeout: Duration, generateToken: () => String): Props =
    Props(classOf[UserSession], sessionId, authInfo, timeout, generateToken)

}

class UserSession(sessionId: UserSession.SessionId,
                  var authInfo: AuthInfo,
                  timeout: Duration,
                  generateToken: () => String) extends Actor {
  import UserSession._

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit =
    refreshTimeout()

  def receive = {
    case ValidateToken(token) =>
      if (token == authInfo.token) {
        authInfo = authInfo.copy(generateToken())
        sender() ! AuthToken(authInfo.token)
      } else {
        sender() ! InvalidToken
      }
      refreshTimeout()
  }

  private[this] def refreshTimeout(): Unit = {
    timeout match {
      case d: FiniteDuration =>
        context.setReceiveTimeout(d)
      case _ =>
        context.setReceiveTimeout(Duration.Undefined)
    }
  }

}
