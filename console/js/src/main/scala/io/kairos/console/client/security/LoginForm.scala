package io.kairos.console.client.security

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, _}

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object LoginForm {
  
  case class LoginInfo(username: String, password: String)

  type LoginHandler = LoginInfo => Callback

  class LoginBackend($: BackendScope[LoginHandler, LoginInfo]) {

    def updateUsername(event: ReactEventI): Callback =
      $.modState(_.copy(username = event.target.value))

    def updatePassword(event: ReactEventI): Callback =
      $.modState(_.copy(password = event.target.value))

    def handleSubmit(event: ReactEventI): Callback =
      event.preventDefaultCB >> $.state.flatMap(loginInfo => $.props.flatMap(handler => handler(loginInfo)))

    def render(info: LoginInfo) =
      <.form(^.onSubmit ==> handleSubmit,
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "username", "Username"),
          <.input.text(^.id := "username", ^.`class` := "form-control", ^.placeholder := "Username",
            ^.required := true, ^.onChange ==> updateUsername, ^.value := info.username
          )
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "password", "Password"),
          <.input.password(^.id := "password", ^.`class` := "form-control", ^.placeholder := "Password",
            ^.required := true, ^.onChange ==> updatePassword, ^.value := info.password
          )
        ),
        <.div(^.`class` := "checkbox",
          <.label(^.`for` := "rememberMe",
            <.input.checkbox(^.id := "rememberMe"),
            "Remember me"
          )
        ),
        <.button(^.`class` := "btn btn-default", "Sign in")
      )
  }

  private[this] val component = ReactComponentB[LoginHandler]("LoginForm").
    initialState(LoginInfo("", "")).
    renderBackend[LoginBackend].
    build

  def apply(loginHandler: LoginHandler) = component(loginHandler)

}
