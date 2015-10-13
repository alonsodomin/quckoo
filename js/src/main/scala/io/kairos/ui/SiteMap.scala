package io.kairos.ui

import io.kairos.ui.login.LoginPage
import japgolly.scalajs.react.extra.router2.{BaseUrl, Redirect, Router, RouterConfigDsl}

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object SiteMap {

  sealed trait ConsolePage

  case object Welcome extends ConsolePage
  case object Home extends ConsolePage
  case object Login extends ConsolePage

  private[this] val publicPages = RouterConfigDsl[ConsolePage].buildRule { dsl =>
    import dsl._

    (emptyRule
    | staticRoute(root, Welcome) ~> render(WelcomePage())
    | staticRoute("#login", Login) ~> render(LoginPage())
    )
  }

  private[this] val privatePages = RouterConfigDsl[ConsolePage].buildRule { dsl =>
    import dsl._

    implicit val redirectMethod = Redirect.Push

    (emptyRule
    | staticRoute("#home", Home) ~> render(HomePage())
    ).addConditionIO(auth.isAuthenticated)(_ => Some(redirectToPage(Login)))
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
