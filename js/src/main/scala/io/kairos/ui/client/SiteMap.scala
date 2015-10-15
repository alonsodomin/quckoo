package io.kairos.ui.client

import io.kairos.ui.client.pages.{HomePage, LoginPage}
import io.kairos.ui.client.security.ClientAuth
import japgolly.scalajs.react.extra.router2.{BaseUrl, Redirect, Router, RouterConfigDsl}

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object SiteMap extends ClientAuth {

  sealed trait ConsolePage

  case object Welcome extends ConsolePage
  case object Home extends ConsolePage
  case object Login extends ConsolePage

  private[this] val publicPages = RouterConfigDsl[ConsolePage].buildRule { dsl =>
    import dsl._

    (emptyRule
    | staticRoute(root, Welcome) ~> redirectToPage(Home)(Redirect.Push)
    | staticRoute("#login", Login) ~> renderR(LoginPage(_))
    )
  }

  private[this] val privatePages = RouterConfigDsl[ConsolePage].buildRule { dsl =>
    import dsl._

    implicit val redirectMethod = Redirect.Push

    (emptyRule
    | staticRoute("#home", Home) ~> render(HomePage())
    ).addConditionIO(isAuthenticated)(_ => Some(redirectToPage(Login)))
  }

  private[this] val config = RouterConfigDsl[ConsolePage].buildConfig { dsl =>
    import dsl._

    (emptyRule
    | publicPages
    | privatePages
    ).notFound(redirectToPage(Welcome)(Redirect.Replace)).
      logToConsole
  }

  val baseUrl = BaseUrl.fromWindowOrigin_/
  val router = Router(baseUrl, config)

}
