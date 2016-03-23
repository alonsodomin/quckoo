package io.quckoo.console.client.security

import io.quckoo.console.client.components._
import io.quckoo.console.protocol.LoginRequest
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

  @Lenses
  case class State(request: LoginRequest)

  class LoginBackend($: BackendScope[LoginHandler, State]) {

    def handleSubmit(event: ReactEventI): Callback = {
      def invokeHandler(loginReq: LoginRequest): Callback =
        $.props.flatMap(handler => handler(loginReq))

      event.preventDefaultCB >> $.state.map(_.request) >>= invokeHandler
    }

  }

  private[this] val component = ReactComponentB[LoginHandler]("LoginForm").
    initialState(State(LoginRequest("", ""))).
    backend(new LoginBackend(_)).
    render { $ =>
      val username = ExternalVar.state($.zoomL(State.request ^|-> LoginRequest.username))
      val password = ExternalVar.state($.zoomL(State.request ^|-> LoginRequest.password))

      def usernameChange(evt: ReactEventI): Callback =
        username.set(evt.target.value)
      def passwordChange(evt: ReactEventI): Callback =
        password.set(evt.target.value)

      <.form(^.name := "loginForm", ^.onSubmit ==> $.backend.handleSubmit,
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "username", ^.`class` := "control-label", "Username"),
          <.input.text(^.id := "username", ^.`class` := "form-control", ^.required := true,
            ^.value := username.value, ^.onChange ==> usernameChange
          )
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "password", ^.`class` := "control-label", "Password"),
          <.input.password(^.id := "password", ^.`class` := "form-control",
            ^.value := password.value, ^.onChange ==> passwordChange
          )
        ),
        Button(Button.Props(style = ContextStyle.primary), Icons.signIn, "Sign in")
      )
    }.
    build

  def apply(loginHandler: LoginHandler) = component(loginHandler)

}
