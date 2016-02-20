package io.kairos.console.client.security

import io.kairos.console.client.components._
import io.kairos.console.client.layout.InputField
import io.kairos.console.protocol.LoginRequest
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, _}
import monocle.macros.Lenses

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object LoginForm {
  import MonocleReact._

  type LoginHandler = LoginRequest => Callback

  class LoginBackend($: BackendScope[LoginHandler, LoginRequest]) {

    def handleSubmit(event: ReactEventI): Callback =
      event.preventDefaultCB >> $.state.flatMap(loginInfo => $.props.flatMap(handler => handler(loginInfo)))

  }

  private[this] val component = ReactComponentB[LoginHandler]("LoginForm").
    initialState(LoginRequest("", "")).
    backend(new LoginBackend(_)).
    render { $ =>
      val username = ExternalVar.state($.zoomL(LoginRequest.username))
      val password = ExternalVar.state($.zoomL(LoginRequest.password))
      <.form(^.name := "loginForm", ^.onSubmit ==> $.backend.handleSubmit,
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "username", "Username"),
          InputField.text("username", "Username", required = true, username)
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "password", "Password"),
          InputField.password("password", "Password", required = true, password)
        ),
        <.div(^.`class` := "checkbox",
          <.label(^.`for` := "rememberMe",
            <.input.checkbox(^.id := "rememberMe"),
            "Remember me"
          )
        ),
        Button(Button.Props(style = ContextStyle.primary), Icons.signIn, "Sign in")
      )
    } build

  def apply(loginHandler: LoginHandler) = component(loginHandler)

}
