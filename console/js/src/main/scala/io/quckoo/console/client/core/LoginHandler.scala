package io.quckoo.console.client.core

import diode.data.{Empty, Pot, Ready}
import diode.{ActionHandler, Effect, ModelRW}
import io.quckoo.auth.User
import io.quckoo.console.protocol._

import scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 20/02/2016.
  */
class LoginHandler[M](model: ModelRW[M, Option[User]]) extends ActionHandler(model) {

  override def handle = {
    case action: LoginRequest =>
      effectOnly(Effect(
        ConsoleClient.login(action.username, action.password).
          map(_ => LoggedIn(action.username))
      ))

    case LogoutRequest =>
      effectOnly(Effect(ConsoleClient.logout().map(_ => LoggedOut)))

    case LoggedIn(username) =>
      updated(Some(User(username)))

    case LoggedOut =>
      updated(None)
  }

}
