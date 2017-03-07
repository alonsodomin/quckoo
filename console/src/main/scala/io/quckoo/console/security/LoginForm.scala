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

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import monocle.macros.Lenses

/**
  * Created by aalonsodominguez on 12/10/2015.
  */
object LoginForm {

  type LoginHandler = (String, String) => Callback

  @Lenses
  case class State(username: Option[String], password: Option[Password])

  class LoginBackend($ : BackendScope[LoginHandler, State]) {

    def onUsernameChange(username: Option[String]): Callback =
      $.modState(_.copy(username = username))

    def onPasswordChange(password: Option[Password]): Callback =
      $.modState(_.copy(password = password))

    def submit(handler: LoginHandler)(event: ReactEventI): Callback = {
      val username = $.state.map(_.username).asCBO[String]
      val password = $.state.map(_.password).asCBO[Password]

      val perform = username.flatMap(u => password.map(p => (u, p))) flatMap {
        case (u, p) => handler(u, p.value)
      } toCallback

      event.preventDefaultCB >> perform
    }

    private[this] val UsernameInput = Input[String]
    private[this] val PasswordInput = Input[Password]

    def render(handler: LoginHandler, state: State) = {
      <.form(
        ^.name := "loginForm",
        ^.onSubmit ==> submit(handler),
        <.div(
          ^.`class` := "form-group",
          <.label(^.`for` := "username", ^.`class` := "control-label", "Username"),
          UsernameInput(state.username, onUsernameChange _, ^.id := "username")),
        <.div(
          ^.`class` := "form-group",
          <.label(^.`for` := "password", ^.`class` := "control-label", "Password"),
          PasswordInput(state.password, onPasswordChange _, ^.id := "password")),
        Button(
          Button.Props(
            style = ContextStyle.primary,
            disabled = state.username.isEmpty || state.password.isEmpty
          ),
          Icons.signIn,
          "Sign in"))
    }

  }

  private[this] val component = ReactComponentB[LoginHandler]("LoginForm")
    .initialState(State(None, None))
    .renderBackend[LoginBackend]
    .build

  def apply(loginHandler: LoginHandler) = component(loginHandler)

}
