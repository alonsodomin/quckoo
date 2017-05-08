package io.quckoo.console.layout

import diode.react.ModelProxy

import io.quckoo.console.ConsoleRoute
import io.quckoo.console.ConsoleRoute.{Dashboard, Registry, Scheduler}
import io.quckoo.console.components.Icons
import io.quckoo.console.core.ConsoleScope
import io.quckoo.console.layout.Navigation.NavigationItem

import japgolly.scalajs.react.extra.router.{Resolution, RouterCtl}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Created by alonsodomin on 08/05/2017.
  */
object Layout {

  final val MainMenu = List(
    NavigationItem(Icons.dashboard, "Dashboard", Dashboard),
    NavigationItem(Icons.book, "Registry", Registry),
    NavigationItem(Icons.clockO, "Scheduler", Scheduler)
  )

  def apply(proxy: ModelProxy[ConsoleScope])(routerCtl: RouterCtl[ConsoleRoute],
                                             resolution: Resolution[ConsoleRoute]): VdomElement = {
    def navigation = proxy.wrap(_.passport.flatMap(_.principal)) { principal =>
      Navigation(MainMenu.head, MainMenu, routerCtl, resolution.page, principal)
    }

    <.div(navigation, resolution.render())
  }

}
