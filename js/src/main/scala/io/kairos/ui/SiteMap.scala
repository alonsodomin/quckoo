package io.kairos.ui

import japgolly.scalajs.react.extra.router2.{BaseUrl, Redirect, Router, RouterConfigDsl}

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object SiteMap {

  sealed trait ConsolePage

  case object Welcome extends ConsolePage
  case object Login extends ConsolePage

  private[this] val config = RouterConfigDsl[ConsolePage].buildConfig { dsl =>
    import dsl._

    (emptyRule
    | staticRoute(root, Welcome) ~> render(WelcomePage())
    | staticRoute("#login", Login) ~> render(LoginPage())
    ).notFound(redirectToPage(Welcome)(Redirect.Replace)).
      logToConsole
  }

  val baseUrl = BaseUrl.fromWindowOrigin_/
  val router = Router(baseUrl, config)

}
