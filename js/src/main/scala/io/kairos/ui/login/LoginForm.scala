package io.kairos.ui.login

import io.kairos.ui.{Api, ClientApi, SiteMap}
import japgolly.scalajs.react.extra.router2._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactEventAliases}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object LoginForm {
  import SiteMap._

  case class Props(api: Api, router: RouterCtl[ConsolePage])
  case class State(username: String, password: String)

  class LoginBackend($: BackendScope[Props, State]) extends ReactEventAliases {

    def handleSubmit(event: ReactEventI) = {
      event.preventDefault()
      $.props.api.login($.get().username, $.get().password).onSuccess { case _ =>
        $.props.router.set(Home) unsafePerformIO()
      }
    }

  }

  private[this] val Factory = ReactComponentB[Props]("LoginForm").
    initialState(State("", "")).
    backend(new LoginBackend(_)).
    render((_, _, b) =>
      <.form(^.onSubmit ==> b.handleSubmit,
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "username", "Username"),
          <.input(^.id := "username", ^.`class` := "form-control", ^.placeholder := "Username", ^.required := true)
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "password", "Password"),
          <.input(^.id := "password", ^.`type` := "password", ^.`class` := "form-control", ^.placeholder := "Password", ^.required := true)
        ),
        <.div(^.`class` := "checkbox",
          <.label(^.`for` := "rememberMe",
            <.input(^.id := "rememberMe", ^.`type` := "checkbox", "Remember me")
          )
        ),
        <.button(^.`class` := "btn btn-default", "Sign in")
      )
    ).build

  def apply(router: RouterCtl[ConsolePage]) = Factory(Props(ClientApi, router))

}
