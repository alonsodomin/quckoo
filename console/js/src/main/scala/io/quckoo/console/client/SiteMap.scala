package io.quckoo.console.client

import io.quckoo.console.client.components.Icons
import io.quckoo.console.client.core.ConsoleCircuit
import io.quckoo.console.client.scheduler.SchedulerPage
import io.quckoo.console.client.layout.Navigation
import io.quckoo.console.client.layout.Navigation.NavigationItem
import io.quckoo.console.client.registry.RegistryPage
import io.quckoo.console.client.security.{ClientAuth, LoginPage}
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object SiteMap extends ClientAuth {

  sealed trait ConsolePage
  case object Root extends ConsolePage
  case object Home extends ConsolePage
  case object Login extends ConsolePage
  case object Registry extends ConsolePage
  case object Scheduler extends ConsolePage

  private[this] val publicPages = RouterConfigDsl[ConsolePage].buildRule { dsl =>
    import dsl._

    (emptyRule
    | staticRoute(root, Root) ~> redirectToPage(Home)(Redirect.Push)
    | staticRoute("#login", Login) ~> renderR(LoginPage(_))
    )
  }

  private[this] val privatePages = RouterConfigDsl[ConsolePage].buildRule { dsl =>
    import dsl._

    implicit val redirectMethod = Redirect.Push

    def registryPage =
      ConsoleCircuit.connect(identity(_))(RegistryPage(_))
    def schedulerPage =
      ConsoleCircuit.connect(identity(_))(SchedulerPage(_))

    (emptyRule
    | staticRoute("#home", Home) ~> render(HomePage())
    | staticRoute("#registry", Registry) ~> render(registryPage)
    | staticRoute("#scheduler", Scheduler) ~> render(schedulerPage)
    ).addCondition(isAuthenticatedC)(referral => Some(renderR(router => LoginPage(router, Some(referral)))))
  }

  private[this] val config = RouterConfigDsl[ConsolePage].buildConfig { dsl =>
    import dsl._

    (emptyRule
    | publicPages
    | privatePages
    ).notFound(redirectToPage(Root)(Redirect.Replace)).
      renderWith(layout).
      logToConsole
  }

  val mainMenu = List(
    NavigationItem(Icons.dashboard, "Dashboard", Home),
    NavigationItem(Icons.book, "Registry", Registry),
    NavigationItem(Icons.clockO, "Scheduler", Scheduler)
  )

  def layout(ctrl: RouterCtl[ConsolePage], res: Resolution[ConsolePage]) =
    <.div(
      if (isAuthenticated) {
        Navigation(mainMenu.head, mainMenu, ctrl, res.page)
      } else EmptyTag,
      res.render()
    )

  val baseUrl = BaseUrl.fromWindowOrigin_/
  val router = Router(baseUrl, config)

}
