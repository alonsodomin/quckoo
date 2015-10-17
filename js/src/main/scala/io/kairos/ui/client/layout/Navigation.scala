package io.kairos.ui.client.layout

import io.kairos.ui.client.SiteMap
import io.kairos.ui.client.core.ClientApi
import io.kairos.ui.client.security.ClientAuth
import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scalaz.effect.IO

/**
 * Created by alonsodomin on 16/10/2015.
 */
object Navigation extends ClientAuth {
  import SiteMap._

  case class NavigationItem(name: String, page: ConsolePage)

  case class Props(initial: NavigationItem, menu: Seq[NavigationItem], routerCtl: RouterCtl[ConsolePage])
  case class State(current: NavigationItem)

  class Backend($: BackendScope[Props, State]) {

    def navigationItemClickedEH(item: NavigationItem): ReactEvent => IO[Unit] =
      e => preventDefaultIO(e) >> stopPropagationIO(e) >> IO {
        $.modState(_.copy(current = item))
      } >> $.props.routerCtl.set(item.page)

    def renderNavItem(item: NavigationItem) = {
      <.li(^.classSet("active" -> ($.get().current == item)),
        <.a(^.href := $.props.routerCtl.urlFor(item.page).value,
          ^.onClick ~~> navigationItemClickedEH(item), item.name)
      )
    }


    def onLogoutClicked: ReactEvent => IO[Unit] = {
      def logoutAndRefresh: IO[Unit] = IO {
        ClientApi.logout() map { case _ =>
          $.props.routerCtl.refresh
        } onSuccess { case io => io.unsafePerformIO() }
      }

      e => preventDefaultIO(e) >> logoutAndRefresh
    }

  }

  private[this] val component = ReactComponentB[Props]("Navigation").
    initialStateP(p => State(p.initial)).
    backend(new Backend(_)).
    render((p, s, b) =>
      <.nav(^.`class` := "navbar navbar-default navbar-fixed-top",
        <.div(^.`class` := "container-fluid",
          <.div(^.`class` := "navbar-header",
            <.a(^.`class` := "navbar-brand", ^.href := p.routerCtl.urlFor(Home).value,
              ^.onClick ~~> b.navigationItemClickedEH(p.initial),
              <.i(^.`class` := "fa fa-home"),
              <.span("Kairos Console")
            )
          ),
          if (isAuthenticated) {
            <.div(^.`class` := "collapse navbar-collapse",
              <.ul(^.`class` := "nav navbar-nav",
                p.menu.map(item => b.renderNavItem(item))
              ),
              <.ul(^.`class` := "nav navbar-nav navbar-right",
                <.li(<.a(^.href := "#", ^.onClick ~~> b.onLogoutClicked, "Logout"))
              )
            )
          } else EmptyTag
        )
      )
    ).build

  def apply(initial: NavigationItem, menu: Seq[NavigationItem], routerCtl: RouterCtl[ConsolePage]) =
    component(Props(initial, menu, routerCtl))

}
