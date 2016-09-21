/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.console

import diode.react.ModelProxy

import io.quckoo.console.components._
import io.quckoo.console.core.{ConsoleCircuit, ConsoleScope, LoginProcessor}
import io.quckoo.console.dashboard.DashboardView
import io.quckoo.console.layout.Navigation
import io.quckoo.console.layout.Navigation.NavigationItem
import io.quckoo.console.registry.RegistryPage
import io.quckoo.console.scheduler.SchedulerPage
import io.quckoo.console.security.LoginPage

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Created by aalonsodominguez on 12/10/2015.
 */
object SiteMap {
  import ConsoleRoute._

  def dashboardPage(proxy: ModelProxy[ConsoleScope]) =
    proxy.wrap(identity(_))(DashboardView(_))

  def loginPage(proxy: ModelProxy[ConsoleScope])(referral: Option[ConsoleRoute]) =
    proxy.wrap(identity(_))(p => LoginPage(p, referral))

  def registryPage(proxy: ModelProxy[ConsoleScope]) =
    proxy.wrap(identity(_))(RegistryPage(_))

  def schedulerPage(proxy: ModelProxy[ConsoleScope]) =
    proxy.wrap(identity(_))(SchedulerPage(_))

  private[this] def publicPages(proxy: ModelProxy[ConsoleScope]) = RouterConfigDsl[ConsoleRoute].buildRule { dsl =>
    import dsl._

    (emptyRule
    | staticRoute(root, RootRoute) ~> redirectToPage(DashboardRoute)(Redirect.Push)
    | staticRoute("#login", LoginRoute) ~> render(loginPage(proxy)(None))
    )
  }

  private[this] def privatePages(proxy: ModelProxy[ConsoleScope]) = RouterConfigDsl[ConsoleRoute].buildRule { dsl =>
    import dsl._

    implicit val redirectMethod = Redirect.Push

    def isLoggedIn: CallbackB =
      CallbackTo { proxy().passport.isDefined }

    def redirectToLogin(referral: ConsoleRoute) =
      Some(render(loginPage(proxy)(Some(referral))))

    (emptyRule
    | staticRoute("#home", DashboardRoute) ~> render(dashboardPage(proxy))
    | staticRoute("#registry", RegistryRoute) ~> render(registryPage(proxy))
    | staticRoute("#scheduler", SchedulerRoute) ~> render(schedulerPage(proxy))
    ).addCondition(isLoggedIn)(redirectToLogin)
  }

  private[this] def config(proxy: ModelProxy[ConsoleScope]) = RouterConfigDsl[ConsoleRoute].buildConfig { dsl =>
    import dsl._

    (emptyRule
    | publicPages(proxy)
    | privatePages(proxy)
    ).notFound(redirectToPage(RootRoute)(Redirect.Replace)).
      renderWith(layout(proxy)).
      verify(ConsoleRoute.all.head, ConsoleRoute.all.tail: _*)
  }

  val mainMenu = List(
    NavigationItem(Icons.dashboard, "Dashboard", DashboardRoute),
    NavigationItem(Icons.book, "Registry", RegistryRoute),
    NavigationItem(Icons.clockO, "Scheduler", SchedulerRoute)
  )

  def layout(proxy: ModelProxy[ConsoleScope])(ctrl: RouterCtl[ConsoleRoute], res: Resolution[ConsoleRoute]): ReactElement = {
    def navigation = proxy.wrap(_.passport.flatMap(_.principal)){ principal =>
      Navigation(mainMenu.head, mainMenu, ctrl, res.page, principal)
    }

    <.div(navigation, res.render())
  }

  val baseUrl = BaseUrl.fromWindowOrigin_/

  def apply(proxy: ModelProxy[ConsoleScope]) = {
    val cfg = config(proxy)
    val logic = new RouterLogic(baseUrl, cfg)
    val processor = new LoginProcessor(logic.ctl)

    val component = Router.componentUnbuiltC(baseUrl, cfg, logic).
      componentWillMount(_ => Callback(ConsoleCircuit.addProcessor(processor))).
      componentWillUnmount(_ => Callback(ConsoleCircuit.removeProcessor(processor))).
      build

    component()
  }

}
