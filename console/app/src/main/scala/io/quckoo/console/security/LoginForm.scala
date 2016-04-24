/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.console.security

import io.quckoo.console.components._
import io.quckoo.protocol.client.SignIn

import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, _}

import monocle.macros.Lenses

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object LoginForm {
  import MonocleReact._

  type LoginHandler = (String, String) => Callback

  @Lenses
  case class State(username: String, password: String)

  class LoginBackend($: BackendScope[LoginHandler, State]) {

    def handleSubmit(event: ReactEventI): Callback = {
      def invokeHandler(state: State): Callback =
        $.props.flatMap(handler => handler(state.username, state.password))

      event.preventDefaultCB >> $.state >>= invokeHandler
    }

  }

  private[this] val component = ReactComponentB[LoginHandler]("LoginForm").
    initialState(State("", "")).
    backend(new LoginBackend(_)).
    render { $ =>
      val username = ExternalVar.state($.zoomL(State.username))
      val password = ExternalVar.state($.zoomL(State.password))

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
