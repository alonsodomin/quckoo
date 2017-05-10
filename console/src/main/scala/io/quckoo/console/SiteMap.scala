/*
 * Copyright 2015 A. Alonso Dominguez
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
import io.quckoo.console.core.{ConsoleCircuit, ConsoleScope, ErrorProcessor, LoginProcessor}
import io.quckoo.console.dashboard.DashboardView
import io.quckoo.console.layout.Navigation
import io.quckoo.console.layout.Navigation.NavigationItem
import io.quckoo.console.registry.RegistryPage
import io.quckoo.console.scheduler.SchedulerPage
import io.quckoo.console.security.LoginPage

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Created by aalonsodominguez on 12/10/2015.
  */
object SiteMap {
  import ConsoleRoute._

  private[this] def publicPages(proxy: ModelProxy[ConsoleScope]) =
    RouterConfigDsl[ConsoleRoute].buildRule { dsl =>
      import dsl._

      (emptyRule
        | staticRoute(root, Root) ~> redirectToPage(Dashboard)(Redirect.Push)
        | staticRoute("#login", Login) ~> render(LoginPage(proxy)))
    }

  private[this] def privatePages(proxy: ModelProxy[ConsoleScope]) =
    RouterConfigDsl[ConsoleRoute].buildRule { dsl =>
      import dsl._

      def isLoggedIn: CallbackTo[Boolean] =
        CallbackTo { proxy().passport.isDefined }

      def redirectToLogin(referral: ConsoleRoute) =
        Some(render(LoginPage(proxy, Some(referral))))

      (emptyRule
        | staticRoute("#home", Dashboard) ~> render(DashboardView(proxy))
        | staticRoute("#registry", Registry) ~> render(RegistryPage(proxy))
        | staticRoute("#scheduler", Scheduler) ~> render(SchedulerPage(proxy)))
        .addCondition(isLoggedIn)(redirectToLogin)
    }

  private[this] def config(proxy: ModelProxy[ConsoleScope]) =
    RouterConfigDsl[ConsoleRoute].buildConfig { dsl =>
      import dsl._

      (emptyRule
        | publicPages(proxy)
        | privatePages(proxy))
        .notFound(redirectToPage(Root)(Redirect.Replace))
        .renderWith(layout(proxy))
        .verify(ConsoleRoute.values.head, ConsoleRoute.values.tail: _*)
    }

  val mainMenu = List(
    NavigationItem(Icons.dashboard, "Dashboard", Dashboard),
    NavigationItem(Icons.book, "Registry", Registry),
    NavigationItem(Icons.clockO, "Scheduler", Scheduler)
  )

  def layout(proxy: ModelProxy[ConsoleScope])(ctrl: RouterCtl[ConsoleRoute],
                                              res: Resolution[ConsoleRoute]): VdomElement = {
    def navigation = proxy.wrap(_.passport.flatMap(_.principal)) { principal =>
      Navigation(mainMenu.head, mainMenu, ctrl, res.page, principal)
    }

    <.div(navigation, res.render())
  }

  val baseUrl = BaseUrl.fromWindowOrigin_/

  def apply(proxy: ModelProxy[ConsoleScope]) = {
    val cfg   = config(proxy)
    val logic = new RouterLogic(baseUrl, cfg)
    val processors = List(
      new LoginProcessor(logic.ctl),
      new ErrorProcessor
    )

    val component = Router
      .componentUnbuiltC(baseUrl, cfg, logic)
      .componentWillMount(_ => Callback(processors.foreach(ConsoleCircuit.addProcessor)))
      .componentWillUnmount(_ => Callback(processors.foreach(ConsoleCircuit.removeProcessor)))
      .build

    component()
  }

}
