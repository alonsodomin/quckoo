package io.quckoo.console.client.layout

import diode.react.ModelProxy
import io.quckoo.console.client.SiteMap
import io.quckoo.console.client.components._
import io.quckoo.console.client.core.{ConsoleCircuit, ConsoleClient, ConsoleScope}
import io.quckoo.console.client.security.{ClientAuth, UserMenu}
import io.quckoo.protocol.client.SignOut
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Created by alonsodomin on 16/10/2015.
 */
object Navigation extends ClientAuth {
  import SiteMap._

  sealed trait NavigationMenu
  case class NavigationList(icon: Icon, name: String, items: List[NavigationMenu]) extends NavigationMenu
  case class NavigationItem(icon: Icon, name: String, route: ConsoleRoute) extends NavigationMenu
  case object NavigationSeparator extends NavigationMenu

  case class Props(initial: NavigationItem, menu: List[NavigationMenu],
                   routerCtl: RouterCtl[ConsoleRoute], current: ConsoleRoute,
                   proxy: ModelProxy[ConsoleScope])

  class Backend($: BackendScope[Props, Unit]) {

    def navigationItemClicked(item: NavigationItem): ReactEvent => Callback =
      e => preventDefault(e) >> stopPropagation(e) >>
        $.props.flatMap(_.routerCtl.set(item.route))

    def renderNavMenu(menu: NavigationMenu, props: Props) = {
      def navItem(item: NavigationItem): ReactNode = {
        <.li(^.classSet("active" -> (props.current == item.route)),
          <.a(^.href := props.routerCtl.urlFor(item.route).value,
            ^.onClick ==> navigationItemClicked(item), item.icon, item.name
          )
        )
      }

      def navSeparator: ReactNode = <.li(^.role := "separator", ^.`class` := "divider")

      def navDropdown(list: NavigationList): ReactNode = {
        <.li(^.classSet("dropdown" -> true),
          <.a(^.href := "#", ^.`class` := "dropdown-toggle", ^.role := "button",
            ^.aria.haspopup := true, ^.aria.expanded := false,
            list.icon, list.name, <.span(^.`class` := "caret")
          ),
          <.ul(^.`class` := "dropdown-menu",
            list.items.map(renderItem)
          )
        )
      }

      def renderItem(menuItem: NavigationMenu): ReactNode = menuItem match {
        case item: NavigationItem => navItem(item)
        case NavigationSeparator  => navSeparator
        case list: NavigationList => navDropdown(list)
      }

      renderItem(menu)
    }

    def onLogoutClicked(e: ReactEventI): Callback =
      preventDefault(e) >> $.props.flatMap(_.proxy.dispatch(SignOut))

    def render(props: Props) =
      <.nav(^.`class` := "navbar navbar-default navbar-fixed-top",
        <.div(^.`class` := "container-fluid",
          <.div(^.`class` := "navbar-header",
            <.a(^.`class` := "navbar-brand", ^.href := props.routerCtl.urlFor(DashboardRoute).value,
              ^.onClick ==> navigationItemClicked(props.initial),
              Icons.home, <.span("Quckoo")
            )
          ),
          <.div(^.`class` := "collapse navbar-collapse",
            <.ul(^.`class` := "nav navbar-nav",
              props.menu.map(item => renderNavMenu(item, props))
            ),
            <.ul(^.`class` := "nav navbar-nav navbar-right",
              <.li(^.`class` := "navbar-text", props.proxy.wrap(_.currentUser)(UserMenu.apply)),
              <.li(<.a(^.href := "#", ^.onClick ==> onLogoutClicked, Icons.signOut, "Logout"))
            )
          )
        )
      )

  }

  private[this] val component = ReactComponentB[Props]("Navigation").
    stateless.
    renderBackend[Backend].
    build

  def apply(initial: NavigationItem, menu: List[NavigationMenu],
            routerCtl: RouterCtl[ConsoleRoute], current: ConsoleRoute,
            proxy: ModelProxy[ConsoleScope]) =
    component(Props(initial, menu, routerCtl, current, proxy))

}
