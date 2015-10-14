package io.kairos.ui.client.login

import io.kairos.ui.Api
import io.kairos.ui.client.{ClientApi, SiteMap}
import japgolly.scalajs.react.extra.router2._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactEventAliases}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scalaz.effect.IO

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object LoginForm {
  import SiteMap._

  case class Props(api: Api, router: RouterCtl[ConsolePage])
  case class State(username: String, password: String)

  class LoginBackend($: BackendScope[Props, State]) extends ReactEventAliases {

    def updateUsername(event: ReactEventI) = {
      $.modState(_.copy(username = event.target.value))
    }

    def updatePassword(event: ReactEventI) = {
      $.modState(_.copy(password = event.target.value))
    }

    def handleSubmit(event: ReactEventI) = {
      event.preventDefault()

      def performLogin(): Future[IO[Unit]] =
        $.props.api.login($.get().username, $.get().password).map { _ =>
          $.props.router.set(Home)
        } recover { case _ =>
          $.props.router.set(Login)
        }

      performLogin() onSuccess { case io => io.unsafePerformIO() }
    }

  }

  private[this] val Factory = ReactComponentB[Props]("LoginForm").
    initialState(State("", "")).
    backend(new LoginBackend(_)).
    render((_, _, b) =>
      <.form(^.onSubmit ==> b.handleSubmit,
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "username", "Username"),
          <.input(^.id := "username", ^.`class` := "form-control", ^.placeholder := "Username", ^.required := true,
            ^.onChange ==> b.updateUsername
          )
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "password", "Password"),
          <.input(^.id := "password", ^.`type` := "password", ^.`class` := "form-control", ^.placeholder := "Password",
            ^.required := true, ^.onChange ==> b.updatePassword
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

  def apply(router: RouterCtl[ConsolePage]) = Factory(Props(ClientApi, router))

}
