package io.kairos.ui

import io.kairos.ui.protocol.{LoginRequest, LoginResponse}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactEventAliases}
import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object LoginForm {

  case class State(username: String, password: String)

  private[this] class LoginBackend($: BackendScope[_, State]) extends ReactEventAliases {

    def handleSubmit(event: ReactEventI) = {
      event.preventDefault()
      Ajax.post("/api/login", write(LoginRequest("hkkdjs", "hidasid"))).foreach { res =>
        val response = read[LoginResponse](res.responseText)
        println(s"Token: ${response.token}")
      }
    }

  }

  private[this] val Factory = ReactComponentB[Unit]("LoginForm").
    initialState(State("", "")).
    backend(new LoginBackend(_)).
    render((_, _, b) =>
      <.form(^.`class` := "pure-form pure-form-aligned", ^.onSubmit ==> b.handleSubmit,
        <.fieldset(
          <.div(^.`class` := "pure-control-group",
            <.label(^.`for` := "username", "Username"),
            <.input(^.name := "username", ^.placeholder := "Username")
          )
        ),
        <.fieldset(
          <.div(^.`class` := "pure-control-group",
            <.label(^.`for` := "password", "Password"),
            <.input(^.name := "password", ^.`type` := "password", ^.placeholder := "Password")
          )
        ),
        <.div(^.`class` := "pure-controls",
          <.label(^.`for` := "rememberMe", ^.`class` := "pure-checkbox",
            <.input(^.id := "rememberMe", ^.`type` := "checkbox", "Remember me")
          ),
          <.button(^.`class` := "pure-button pure-button-primary", "Sign in")
        )
      )
    ).buildU

  def apply() = Factory()

}
