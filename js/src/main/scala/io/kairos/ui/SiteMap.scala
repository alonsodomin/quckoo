package io.kairos.ui

import japgolly.scalajs.react.extra.router2.RouterConfigDsl

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object SiteMap {

  sealed trait ConsolePages

  case object Home extends ConsolePages
  case object Login extends ConsolePages

  val config = RouterConfigDsl[ConsolePages].buildConfig { dsl =>
    import dsl._

    (emptyRule
    | staticRoute(root, Login) ~> render(???)
    ).notFound(???)
  }

}
