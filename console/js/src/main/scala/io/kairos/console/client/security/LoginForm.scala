package io.kairos.console.client.security

import io.kairos.{Fault, Required}
import io.kairos.console.client.components._
import io.kairos.console.client.layout.InputField
import io.kairos.console.client.validation._
import io.kairos.console.protocol.LoginRequest
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, _}
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.channels.PublishChannel
import monifu.reactive.subjects.PublishSubject
import monocle.macros.Lenses
import monifu.concurrent.Implicits.globalScheduler

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object LoginForm {
  import MonocleReact._

  type LoginHandler = LoginRequest => Callback

  class LoginBackend($: BackendScope[LoginHandler, LoginRequest]) {

    val subject = PublishSubject[String]()
    val valid = PublishSubject[Boolean]()

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
        FormGroup($.backend.valid,
          <.label(^.`for` := "username", "Username"),
          Input.text(username, $.backend.subject,
            ^.id := "username", ^.placeholder := "Username"),
          ValidationHelp($.backend.subject, notEmptyStr("username"), $.backend.valid)
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "password", "Password"),
          InputField.password("password", "Password", required = true, password)
        ),
        Button(Button.Props(style = ContextStyle.primary), Icons.signIn, "Sign in")
      )
    }.componentWillUnmount($ => Callback {
      $.backend.subject.onComplete()
      $.backend.valid.onComplete()
    }).
    build

  def apply(loginHandler: LoginHandler) = component(loginHandler)

}
