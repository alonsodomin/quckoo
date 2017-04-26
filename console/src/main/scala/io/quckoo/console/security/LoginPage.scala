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

import diode.react.ModelProxy

import io.quckoo.console.ConsoleRoute
import io.quckoo.console.components._
import io.quckoo.console.core.{ConsoleScope, Login}
import io.quckoo.console.layout.ContextStyle
import io.quckoo.console.layout.CssSettings._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 13/10/2015.
  */
object LoginPage {

  object Style extends StyleSheet.Inline {
    import dsl._

    val formPlacement = style(
      width(350 px),
      height(300 px),
      position.absolute,
      left(50 %%),
      top(50 %%),
      marginLeft(-150 px),
      marginTop(-180 px)
    )
  }

  case class Props(proxy: ModelProxy[ConsoleScope], referral: Option[ConsoleRoute])

  class LoginBackend($ : BackendScope[Props, Unit]) {

    def loginHandler(props: Props)(username: String, password: String): Callback =
      props.proxy.dispatchCB(Login(username, password, props.referral))

    def render(props: Props) =
      <.div(
        Style.formPlacement,
        Panel(
          Panel.Props("Quckoo Console - Sign in", ContextStyle.primary),
          LoginForm(loginHandler(props))))
  }

  private[this] val component =
    ScalaComponent.builder[Props]("LoginPage").stateless.renderBackend[LoginBackend].build

  def apply(proxy: ModelProxy[ConsoleScope], referral: Option[ConsoleRoute] = None) =
    component(Props(proxy, referral))

}
