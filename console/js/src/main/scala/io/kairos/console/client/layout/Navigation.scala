package io.kairos.console.client.layout

import io.kairos.console.client.SiteMap
import io.kairos.console.client.core.ClientApi
import io.kairos.console.client.security.ClientAuth
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Created by alonsodomin on 16/10/2015.
 */
object Navigation extends ClientAuth {
  import SiteMap._

  case class NavigationItem(name: String, page: ConsolePage)

  case class Props(initial: NavigationItem, menu: Seq[NavigationItem], routerCtl: RouterCtl[ConsolePage])
  case class State(current: NavigationItem)

  class Backend($: BackendScope[Props, State]) {

    def navigationItemClicked(item: NavigationItem): ReactEvent => Callback =
      e => preventDefault(e) >> stopPropagation(e) >>
        $.modState(_.copy(current = item)) >>
        $.props.flatMap(_.routerCtl.set(item.page))

    def renderNavItem(item: NavigationItem, props: Props, state: State) = {
      <.li(^.classSet("active" -> (state.current == item)),
        <.a(^.href := props.routerCtl.urlFor(item.page).value,
          ^.onClick ==> navigationItemClicked(item), item.name)
      )
    }

    def onLogoutClicked(e: ReactEventI): Callback = {
      def logoutAndRefresh: Callback = Callback {
        ClientApi.logout() map { _ => $.props.map(_.routerCtl.refresh) } recover {
          case error: Throwable => Callback.alert(error.getMessage)
        } onSuccess {
          case cb => cb.runNow()
        }
      }

      preventDefault(e) >> logoutAndRefresh
    }

    def render(props: Props, state: State) =
      <.nav(^.`class` := "navbar navbar-default navbar-fixed-top",
        <.div(^.`class` := "container-fluid",
          <.div(^.`class` := "navbar-header",
            <.a(^.`class` := "navbar-brand", ^.href := props.routerCtl.urlFor(Home).value,
              ^.onClick ==> navigationItemClicked(props.initial),
              <.i(^.`class` := "fa fa-home"),
              <.span("Kairos Console")
            )
          ),
          if (isAuthenticated) {
            <.div(^.`class` := "collapse navbar-collapse",
              <.ul(^.`class` := "nav navbar-nav",
                props.menu.map(item => renderNavItem(item, props, state))
              ),
              <.ul(^.`class` := "nav navbar-nav navbar-right",
                <.li(<.a(^.href := "#", ^.onClick ==> onLogoutClicked, "Logout"))
              )
            )
          } else EmptyTag
        )
      )

  }

  private[this] val component = ReactComponentB[Props]("Navigation").
    initialState_P(p => State(p.initial)).
    renderBackend[Backend].
    build

  def apply(initial: NavigationItem, menu: Seq[NavigationItem], routerCtl: RouterCtl[ConsolePage]) =
    component(Props(initial, menu, routerCtl))

}
