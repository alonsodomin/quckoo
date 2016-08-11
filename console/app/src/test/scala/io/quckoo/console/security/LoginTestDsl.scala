package io.quckoo.console.security

import monocle.macros.Lenses

import japgolly.scalajs.react.test._

/**
  * Created by alonsodomin on 10/07/2016.
  */
object LoginTestDsl /*{
  import LoginFormTestState._
  import ReactTestUtils._

  @Lenses
  final case class LoginState(
    username: String,
    password: String,
    submitted: Boolean = false)

  val dsl = Dsl[Unit, LoginObserver, LoginState]

  def setUsername(username: String): dsl.Actions =
    dsl.action(s"Set username as $username")(ChangeEventData(username) simulate _.obs.usernameInput).
      updateState(LoginState.username.set(username))

  def setPassword(password: String): dsl.Actions =
    dsl.action(s"Set password as $password")(ChangeEventData(password) simulate _.obs.passwordInput).
      updateState(LoginState.password.set(password))

  def submitForm(): dsl.Actions =
    dsl.action("Submit login form")(Simulate click _.obs.submitButton).
      updateState(LoginState.submitted.set(true))

}
*/