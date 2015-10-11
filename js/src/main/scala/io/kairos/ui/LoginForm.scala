package io.kairos.ui

import io.kairos.ui.protocol.{LoginResponse, LoginRequest}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, ReactEventAliases, BackendScope}
import org.scalajs.dom.ext.Ajax
import scalajs.concurrent.JSExecutionContext.Implicits.runNow
import upickle.default._

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object LoginForm {

  private[this] class LoginBackend($: BackendScope[_, Unit]) extends ReactEventAliases {

    def handleSubmit(event: ReactEventI) = {
      event.preventDefault()
      Ajax.post("/ap/login", write(LoginRequest("balbal", "hidasid"))).foreach { res =>
        val response = read[LoginResponse](res.responseText)
        println(s"Token: ${response.token}")
      }
    }

  }

  private[this] val Factory = ReactComponentB[Unit]("LoginForm").
    initialState(()).
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
