package io.kairos.console.client.core

import io.kairos.console.auth.User
import io.kairos.console.client.security.ClientAuth

/**
  * Created by alonsodomin on 20/02/2016.
  */
case class KairosModel private (currentUser: Option[User])

case class LoggedIn(username: String)
case object LoggedOut

object KairosModel extends ClientAuth {
  def initial = KairosModel(authInfo.map(auth => User(auth.userId)))
}
