/*
 * Copyright 2015 A. Alonso Dominguez
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

import io.quckoo.console.test.ConsoleTestExports

import monocle.macros.Lenses

import japgolly.scalajs.react.test._

/**
  * Created by alonsodomin on 10/07/2016.
  */
object LoginTestDsl {
  import ConsoleTestExports._
  import ReactTestUtils._

  @Lenses
  final case class LoginState(username: String, password: String, submitted: Boolean = false)

  val dsl = Dsl[Unit, LoginObserver, LoginState]

  def setUsername(username: String): dsl.Actions =
    dsl
      .action(s"Set username as $username")(SimEvent.Change(username) simulate _.obs.usernameInput)
      .updateState(LoginState.username.set(username))

  def setPassword(password: String): dsl.Actions =
    dsl
      .action(s"Set password as $password")(SimEvent.Change(password) simulate _.obs.passwordInput)
      .updateState(LoginState.password.set(password))

  def submitForm(): dsl.Actions =
    dsl
      .action("Submit login form")(Simulate click _.obs.submitButton)
      .updateState(LoginState.submitted.set(true))

}
