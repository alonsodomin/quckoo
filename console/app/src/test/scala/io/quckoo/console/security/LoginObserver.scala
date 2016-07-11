package io.quckoo.console.security

import LoginTestState._

import org.scalajs.dom.html

/**
  * Created by alonsodomin on 10/07/2016.
  */
class LoginObserver($: HtmlDomZipper) {

  val usernameInput = $("#username").domAs[html.Input]
  val passwordInput = $("#password").domAs[html.Input]
  val submitButton  = $("button:contains('Sign in')").domAs[html.Button]

}
