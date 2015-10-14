package io.kairos.ui.server.core

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}
import io.kairos.ui.auth.UserId
import io.kairos.ui.server.security.AuthInfo

import scala.concurrent.duration.FiniteDuration

/**
 * Created by alonsodomin on 14/10/2015.
 */
object UserManager {

  case class Authenticate(userId: UserId, password: Array[Char])
  case class AuthenticationSuccess(authInfo: AuthInfo)
  case object AuthenticationFailed

  def props(sessionTimeout: FiniteDuration): Props = Props(classOf[UserManager], sessionTimeout)

}

class UserManager(sessionTimeout: FiniteDuration) extends Actor {
  import UserManager._
  import UserSession._

  private[this] var sessionMap = Map.empty[SessionId, ActorRef]

  def receive = {
    case Authenticate(userId, password) =>
      if (userId == "admin" && password.mkString == "password") {
        val sessionId = UUID.randomUUID()
        val token = generateToken()
        val authInfo = new AuthInfo(userId, token)
        val session = context.watch(context.actorOf(UserSession.props(sessionId, authInfo, sessionTimeout, generateToken)))
        sessionMap += (sessionId -> session)
        sender() ! AuthenticationSuccess(authInfo)
      } else {
        sender() ! AuthenticationFailed
      }
  }

  def generateToken: () => String = { UUID.randomUUID().toString }
  
}
