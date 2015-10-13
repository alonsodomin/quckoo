package io.kairos.ui

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactEventAliases}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object LoginForm {

  case class State(username: String, password: String)

  class LoginBackend($: BackendScope[Api, State]) extends ReactEventAliases {

    def handleSubmit(event: ReactEventI) = {
      event.preventDefault()
      $.props.login($.get().username, $.get().password).onSuccess { case token: String =>
        println("Token: " + token)
      }
    }

  }

  private[this] val Factory = ReactComponentB[Api]("LoginForm").
    initialState(State("", "")).
    backend(new LoginBackend(_)).
    render((_, _, b) =>
      <.form(^.onSubmit ==> b.handleSubmit,
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "username", "Username"),
          <.input(^.id := "username", ^.`class` := "form-control", ^.placeholder := "Username")
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "password", "Password"),
          <.input(^.id := "password", ^.`type` := "password", ^.`class` := "form-control", ^.placeholder := "Password")
        ),
        <.div(^.`class` := "checkbox",
          <.label(^.`for` := "rememberMe",
            <.input(^.id := "rememberMe", ^.`type` := "checkbox", "Remember me")
          )
        ),
        <.button(^.`class` := "btn btn-default", "Sign in")
      )
    ).build

  def apply() = Factory(ClientApi)

}
