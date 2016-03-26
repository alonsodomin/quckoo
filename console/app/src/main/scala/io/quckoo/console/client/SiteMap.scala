package io.quckoo.console.client

import io.quckoo.console.client.components.Icons
import io.quckoo.console.client.core.{ConsoleCircuit, LoginProcessor}
import io.quckoo.console.client.scheduler.SchedulerPageView
import io.quckoo.console.client.layout.Navigation
import io.quckoo.console.client.layout.Navigation.NavigationItem
import io.quckoo.console.client.registry.RegistryPageView
import io.quckoo.console.client.security.{ClientAuth, LoginPageView}
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object SiteMap extends ClientAuth {

  sealed trait ConsoleRoute
  case object RootRoute extends ConsoleRoute
  case object DashboardRoute extends ConsoleRoute
  case object LoginRoute extends ConsoleRoute
  case object RegistryRoute extends ConsoleRoute
  case object SchedulerRoute extends ConsoleRoute

  private[this] val publicPages = RouterConfigDsl[ConsoleRoute].buildRule { dsl =>
    import dsl._

    (emptyRule
    | staticRoute(root, RootRoute) ~> redirectToPage(DashboardRoute)(Redirect.Push)
    | staticRoute("#login", LoginRoute) ~> renderR(LoginPageView(_))
    )
  }

  private[this] val privatePages = RouterConfigDsl[ConsoleRoute].buildRule { dsl =>
    import dsl._

    implicit val redirectMethod = Redirect.Push

    def registryPage =
      ConsoleCircuit.connect(identity(_))(RegistryPageView(_))
    def schedulerPage =
      ConsoleCircuit.connect(identity(_))(SchedulerPageView(_))

    (emptyRule
    | staticRoute("#home", DashboardRoute) ~> render(DashboardView())
    | staticRoute("#registry", RegistryRoute) ~> render(registryPage)
    | staticRoute("#scheduler", SchedulerRoute) ~> render(schedulerPage)
    ).addCondition(isAuthenticatedC)(referral => Some(renderR(router => LoginPageView(router, Some(referral)))))
  }

  private[this] val config = RouterConfigDsl[ConsoleRoute].buildConfig { dsl =>
    import dsl._

    (emptyRule
    | publicPages
    | privatePages
    ).notFound(redirectToPage(RootRoute)(Redirect.Replace)).
      renderWith(layout).
      logToConsole
  }

  val mainMenu = List(
    NavigationItem(Icons.dashboard, "Dashboard", DashboardRoute),
    NavigationItem(Icons.book, "Registry", RegistryRoute),
    NavigationItem(Icons.clockO, "Scheduler", SchedulerRoute)
  )

  def layout(ctrl: RouterCtl[ConsoleRoute], res: Resolution[ConsoleRoute]) =
    <.div(
      if (isAuthenticated) {
        ConsoleCircuit.wrap(identity(_))(proxy => Navigation(mainMenu.head, mainMenu, ctrl, res.page, proxy))
      } else EmptyTag,
      res.render()
    )

  val baseUrl = BaseUrl.fromWindowOrigin_/
  val router = {
    val logic = new RouterLogic(baseUrl, config)
    val processor = new LoginProcessor(logic.ctl)
    val customRouter = Router.componentUnbuiltC(baseUrl, config, logic).
      componentWillMount(_ => Callback(ConsoleCircuit.addProcessor(processor))).
      componentWillUnmount(_ => Callback(ConsoleCircuit.removeProcessor(processor))).
      buildU
    customRouter
  }

}
