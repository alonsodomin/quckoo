package io.kairos.console.client.core

import diode.data.{Empty, Pot, Ready}
import diode.{ActionHandler, Effect, ModelRW}
import io.kairos.console.auth.User
import io.kairos.console.protocol._
import scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 20/02/2016.
  */
class LoginHandler[M](model: ModelRW[M, Option[User]]) extends ActionHandler(model) {

  override def handle = {
    case action: LoginRequest =>
      effectOnly(Effect(
        ClientApi.login(action.username, action.password).
          map(_ => LoggedIn(action.username))
      ))

    case LogoutRequest =>
      effectOnly(Effect(ClientApi.logout().map(_ => LoggedOut)))

    case LoggedIn(username) =>
      updated(Some(User(username)))

    case LoggedOut =>
      updated(None)
  }

}
