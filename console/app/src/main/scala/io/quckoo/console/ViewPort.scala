package io.quckoo.console

import diode.react.ModelProxy
import io.quckoo.console.components.Icons
import io.quckoo.console.core.{ConsoleCircuit, ConsoleScope}
import io.quckoo.console.layout.Navigation
import io.quckoo.console.layout.Navigation.NavigationItem
import io.quckoo.console.registry.RegistryPageView
import io.quckoo.console.scheduler.SchedulerPageView
import io.quckoo.console.security.LoginPageView
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import japgolly.scalajs.react.extra.router.{Redirect, Resolution, RouterConfigDsl, RouterCtl}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 26/03/2016.
  */
object ViewPort {
  import ConsoleRoute._

  val mainMenu = List(
    NavigationItem(Icons.dashboard, "Dashboard", DashboardRoute),
    NavigationItem(Icons.book, "Registry", RegistryRoute),
    NavigationItem(Icons.clockO, "Scheduler", SchedulerRoute)
  )

  case class Props(proxy: ModelProxy[ConsoleScope],
                   ctrl: RouterCtl[ConsoleRoute],
                   resolution: Resolution[ConsoleRoute])

  class Backend($: BackendScope[Props, Unit]) {

    def render(props: Props) = {

      def navigation = props.proxy.connect(identity(_)){ proxy =>
        Navigation(mainMenu.head, mainMenu, props.ctrl, props.resolution.page, proxy)
      }

      <.div(
        if (props.proxy().currentUser.isDefined) {
          navigation
        } else EmptyTag,
        props.resolution.render()
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("ViewPort").
    stateless.
    renderBackend[Backend].
    build

  def apply(proxy: ModelProxy[ConsoleScope], ctrl: RouterCtl[ConsoleRoute], resolution: Resolution[ConsoleRoute]) =
    component(Props(proxy, ctrl, resolution))

}
