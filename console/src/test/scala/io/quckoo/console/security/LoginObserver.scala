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

import io.quckoo.console.security.LoginFormTestState._

import org.scalajs.dom.html

/**
  * Created by alonsodomin on 10/07/2016.
  */
class LoginObserver($: HtmlDomZipper) {

  val usernameInput = $("#username").domAs[html.Input]
  val passwordInput = $("#password").domAs[html.Input]
  val submitButton  = $("button:contains('Sign in')").domAs[html.Button]

  val emptyUsername = usernameInput.value.isEmpty
  val emptyPassword = passwordInput.value.isEmpty
  val canSubmit = !submitButton.disabled.getOrElse(false)

}
