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
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 09/07/2016.
  */
object NavBar {

  private[this] final case class NavItemProps(
      title: Symbol,
      selected: Boolean,
      onClick: Symbol => Callback
  )

  private[this] val NavItem = ScalaComponent.builder[NavItemProps]("NavItem")
    .stateless
    .render_P {
      case NavItemProps(title, selected, onClick) =>
        <.li(
          ^.id := title.name,
          ^.role := "presentation",
          (^.`class` := "active").when(selected),
          <.a(^.onClick --> onClick(title), title.name))
    }
    .build

  private[this] val NavBody = ScalaComponent.builder[PropsChildren]("NavBody")
    .stateless
    .render_P { children => <.div(children) }
    .build

  final case class Props(
      items: Seq[Symbol],
      initial: Symbol,
      onClick: Symbol => Callback,
      style: NavStyle.Value = NavStyle.tabs,
      addStyles: Seq[StyleA] = Seq()
  )
  final case class State(selected: Option[Symbol] = None)

  class Backend($ : BackendScope[Props, State]) {

    def tabClicked(props: Props)(title: Symbol): Callback =
      $.modState(_.copy(selected = Some(title))).flatMap(_ => props.onClick(title))

    def render(props: Props, children: PropsChildren, state: State) = {
      val currentTab = state.selected.getOrElse(props.initial)
      <.div(
        <.ul(lookAndFeel.nav(props.style), props.addStyles.toTagMod, props.items.map { title =>
          NavItem.withKey(s"nav-bar_nav-item_$title")(
            NavItemProps(title, currentTab == title, tabClicked(props))
          )
        } toVdomArray),
        NavBody(children)
      )
    }

  }

  private[this] val component = ScalaComponent.builder[Props]("NavBar")
    .initialState(State())
    .renderBackendWithChildren[Backend]
    .build

  def apply(
      items: Seq[Symbol],
      initial: Symbol,
      onClick: Symbol => Callback,
      style: NavStyle.Value = NavStyle.tabs,
      addStyles: Seq[StyleA] = Seq()
    ) = component(Props(items, initial, onClick, style, addStyles)) _

  def apply(props: Props, children: VdomNode*) =
    component(props)(children: _*)

}
