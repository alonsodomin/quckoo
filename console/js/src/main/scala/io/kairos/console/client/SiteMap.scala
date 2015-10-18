package io.kairos.console.client

import io.kairos.console.client.execution.ExecutionsPage
import io.kairos.console.client.layout.Navigation.NavigationItem
import io.kairos.console.client.layout.{Footer, Navigation}
import io.kairos.console.client.registry.RegistryPage
import io.kairos.console.client.security.{ClientAuth, LoginPage}
import japgolly.scalajs.react.extra.router2._
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
  case object Executions extends ConsolePage

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

    (emptyRule
    | staticRoute("#home", Home) ~> render(HomePage())
    | staticRoute("#registry", Registry) ~> render(RegistryPage())
    | staticRoute("#executions", Executions) ~> render(ExecutionsPage())
    ).addConditionIO(isAuthenticatedIO)(_ => Some(redirectToPage(Login)))
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

  val mainMenu = Seq(
    NavigationItem("Home", Home),
    NavigationItem("Registry", Registry),
    NavigationItem("Executions", Executions)
  )

  def layout(ctrl: RouterCtl[ConsolePage], res: Resolution[ConsolePage]) =
    <.div(
      Navigation(mainMenu.head, mainMenu, ctrl),
      res.render(),
      Footer()
    )

  val baseUrl = BaseUrl.fromWindowOrigin_/
  val router = Router(baseUrl, config)

}
