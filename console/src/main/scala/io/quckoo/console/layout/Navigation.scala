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

package io.quckoo.console.layout

import diode.react.ModelProxy

import io.quckoo.auth.Principal
import io.quckoo.console.ConsoleRoute
import io.quckoo.console.components._
import io.quckoo.console.core.ConsoleCircuit.Implicits.consoleClock
import io.quckoo.console.core.Logout
import io.quckoo.console.security.PrincipalWidget

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Created by alonsodomin on 16/10/2015.
  */
object Navigation {
  import ConsoleRoute.Dashboard

  sealed trait NavigationMenu extends Serializable with Product
  case class NavigationList(icon: Icon, name: String, items: List[NavigationMenu])
      extends NavigationMenu
  case class NavigationItem(icon: Icon, name: String, route: ConsoleRoute) extends NavigationMenu
  case object NavigationSeparator                                          extends NavigationMenu

  case class Props(initial: NavigationItem,
                   menu: List[NavigationMenu],
                   routerCtl: RouterCtl[ConsoleRoute],
                   current: ConsoleRoute,
                   proxy: ModelProxy[Option[Principal]])

  class Backend($ : BackendScope[Props, Unit]) {

    def navigationItemClicked(item: NavigationItem): ReactEvent => Callback =
      e =>
        e.preventDefaultCB >> e.stopPropagationCB >>
          $.props.flatMap(_.routerCtl.set(item.route))

    def renderNavMenu(menu: NavigationMenu, props: Props) = {
      def navItem(item: NavigationItem): VdomNode =
        <.li(
          ^.classSet("active" -> (props.current == item.route)),
          ^.key := s"nav-time-${item.name}",
          <.a(
            ^.href := props.routerCtl.urlFor(item.route).value,
            ^.onClick ==> navigationItemClicked(item),
            item.icon,
            item.name
          )
        )

      def navSeparator: VdomNode =
        <.li(^.role := "separator", ^.`class` := "divider")

      def navDropdown(list: NavigationList): VdomNode =
        <.li(
          ^.classSet("dropdown" -> true),
          ^.key := s"nav-submenu-${list.name}",
          <.a(
            ^.href := "#",
            ^.`class` := "dropdown-toggle",
            ^.role := "button",
            ^.aria.hasPopup := true,
            ^.aria.expanded := false,
            list.icon,
            list.name,
            <.span(^.`class` := "caret")
          ),
          <.ul(^.`class` := "dropdown-menu", list.items.toVdomArray(renderItem))
        )

      def renderItem(menuItem: NavigationMenu): VdomNode = menuItem match {
        case item: NavigationItem => navItem(item)
        case NavigationSeparator  => navSeparator
        case list: NavigationList => navDropdown(list)
      }

      renderItem(menu)
    }

    def onLogoutClicked(e: ReactEventFromInput): Callback =
      e.preventDefaultCB >> $.props.flatMap(_.proxy.dispatchCB(Logout))

    def render(props: Props): VdomElement =
      <.div(
        props
          .proxy()
          .map { principal =>
            <.nav(
              ^.`class` := "navbar navbar-default navbar-fixed-top",
              <.div(
                ^.`class` := "container-fluid",
                <.div(
                  ^.`class` := "navbar-header",
                  <.a(
                    ^.`class` := "navbar-brand",
                    ^.href := props.routerCtl.urlFor(Dashboard).value,
                    ^.onClick ==> navigationItemClicked(props.initial),
                    Icons.home,
                    "Quckoo"
                  )
                ),
                <.div(
                  ^.`class` := "collapse navbar-collapse",
                  <.ul(
                    ^.`class` := "nav navbar-nav",
                    props.menu
                      .map(item => renderNavMenu(item, props))
                      .toVdomArray
                  ),
                  <.ul(
                    ^.`class` := "nav navbar-nav navbar-right",
                    <.li(^.`class` := "navbar-text", ClockWidget(consoleClock)),
                    <.li(^.`class` := "navbar-text", PrincipalWidget(principal)),
                    <.li(<.a(^.href := "#", ^.onClick ==> onLogoutClicked, Icons.signOut, "Logout"))
                  )
                )
              )
            )
          }
          .whenDefined
      )

  }

  private[this] val component = ScalaComponent
    .builder[Props]("Navigation")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(initial: NavigationItem,
            menu: List[NavigationMenu],
            routerCtl: RouterCtl[ConsoleRoute],
            current: ConsoleRoute,
            proxy: ModelProxy[Option[Principal]]) =
    component(Props(initial, menu, routerCtl, current, proxy))

}
