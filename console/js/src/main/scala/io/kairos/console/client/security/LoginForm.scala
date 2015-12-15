package io.kairos.console.client.security

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, _}

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object LoginForm {
  
  case class LoginInfo(username: String, password: String)

  type LoginHandler = LoginInfo => Unit

  class LoginBackend($: BackendScope[LoginHandler, LoginInfo]) {

    def updateUsername(event: ReactEventI) = {
      $.modState(_.copy(username = event.target.value))
    }

    def updatePassword(event: ReactEventI) = {
      $.modState(_.copy(password = event.target.value))
    }

    def handleSubmit(event: ReactEventI) = {
      event.preventDefault()
      $.props($.get())
    }

  }

  private[this] val component = ReactComponentB[LoginHandler]("LoginForm").
    initialState(LoginInfo("", "")).
    backend(new LoginBackend(_)).
    render((_, info, b) =>
      <.form(^.onSubmit ==> b.handleSubmit,
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "username", "Username"),
          <.input(^.id := "username", ^.`class` := "form-control", ^.placeholder := "Username",
            ^.required := true, ^.onChange ==> b.updateUsername, ^.value := info.username
          )
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "password", "Password"),
          <.input(^.id := "password", ^.`type` := "password", ^.`class` := "form-control", ^.placeholder := "Password",
            ^.required := true, ^.onChange ==> b.updatePassword, ^.value := info.password
          )
        ),
        <.div(^.`class` := "checkbox",
          <.label(^.`for` := "rememberMe",
            <.input(^.id := "rememberMe", ^.`type` := "checkbox", "Remember me")
          )
        ),
        <.button(^.`class` := "btn btn-default", "Sign in")
      )
    ).build

  def apply(loginHandler: LoginHandler) = component(loginHandler)

}