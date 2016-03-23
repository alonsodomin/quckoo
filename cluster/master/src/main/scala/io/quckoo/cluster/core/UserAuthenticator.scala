package io.quckoo.cluster.core

import akka.actor.{Actor, Props}
import io.quckoo.auth.AuthInfo
import io.quckoo.auth.UserId
import io.quckoo.security._

import scala.concurrent.duration.FiniteDuration

/**
 * Created by alonsodomin on 14/10/2015.
 */
object UserAuthenticator {

  case class Authenticate(userId: UserId, password: Array[Char])
  case class AuthenticationSuccess(authInfo: AuthInfo)
  case object AuthenticationFailed

  def props(sessionTimeout: FiniteDuration): Props = Props(classOf[UserAuthenticator], sessionTimeout)

}

class UserAuthenticator(sessionTimeout: FiniteDuration) extends Actor {
  import UserAuthenticator._

  def receive = {
    case Authenticate(userId, password) =>
      if (userId == "admin" && password.mkString == "password") {
        val authInfo = new AuthInfo(userId, generateAuthToken)
        sender() ! AuthenticationSuccess(authInfo)
      } else {
        sender() ! AuthenticationFailed
      }
  }
  
}
