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

  def props(sessionId: SessionId, authInfo: AuthInfo, timeout: Duration, generateToken: () => String): Props =
    Props(classOf[UserSession], sessionId, authInfo, timeout, generateToken)

}

class UserSession(sessionId: UserSession.SessionId,
                  authInfo: AuthInfo,
                  timeout: Duration,
                  generateToken: () => String) extends Actor {
  import UserSession._

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    timeout match {
      case d: FiniteDuration =>
        context.setReceiveTimeout(d)
      case _ =>
        context.setReceiveTimeout(Duration.Undefined)
    }
  }

  def receive = {
    case ValidateToken(token) =>

  }

}
