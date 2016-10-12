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

package io.quckoo.console.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 09/07/2016.
  */
object NavBar {

  private[this] final case class NavItemProps(
      title: String,
      selected: Boolean,
      onClick: String => Callback
  )

  private[this] val NavItem = ReactComponentB[NavItemProps]("NavItem").stateless.render_P {
    case NavItemProps(title, selected, onClick) =>
      <.li(
        ^.id := title,
        ^.role := "presentation",
        selected ?= (^.`class` := "active"),
        <.a(^.onClick --> onClick(title), title))
  } build

  private[this] val NavBody = ReactComponentB[PropsChildren]("NavBody").stateless.render_P {
    children =>
      <.div(children)
  } build

  final case class Props(
      items: Seq[String],
      initial: String,
      onClick: String => Callback,
      style: NavStyle.Value = NavStyle.tabs,
      addStyles: Seq[StyleA] = Seq()
  )
  final case class State(selected: Option[String] = None)

  class Backend($ : BackendScope[Props, State]) {

    def tabClicked(props: Props)(title: String): Callback =
      $.modState(_.copy(selected = Some(title))).flatMap(_ => props.onClick(title))

    def render(props: Props, state: State) = {
      val currentTab = state.selected.getOrElse(props.initial)
      <.div(
        <.ul(lookAndFeel.nav(props.style), props.addStyles, props.items.map { title =>
          NavItem.withKey(s"nav-item-$title")(
            NavItemProps(title, currentTab == title, tabClicked(props))
          )
        }),
        NavBody.withKey("nav-panel-body")($.propsChildren.runNow())
      )
    }

  }

  private[this] val component =
    ReactComponentB[Props]("NavBar").initialState(State()).renderBackend[Backend].build

  def apply(props: Props, children: ReactNode*) =
    component(props, children: _*)

}
