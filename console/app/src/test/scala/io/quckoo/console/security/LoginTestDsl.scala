package io.quckoo.console.security

import monocle.macros.Lenses

import japgolly.scalajs.react.test._

/**
  * Created by alonsodomin on 10/07/2016.
  */
object LoginTestDsl {
  import LoginTestState._
  import ReactTestUtils._

  @Lenses
  final case class State(username: String, password: String)

  val dsl = Dsl[Unit, LoginObserver, State]

  def setUsername(username: String): dsl.Actions =
    dsl.action(s"Set username as $username")(ChangeEventData(username) simulate _.obs.usernameInput)

  def setPassword(password: String): dsl.Actions =
    dsl.action(s"Set password as $password")(ChangeEventData(password) simulate _.obs.passwordInput)

  def submitForm(): dsl.Actions =
    dsl.action("Submit login form")(Simulate click _.obs.submitButton)

}
