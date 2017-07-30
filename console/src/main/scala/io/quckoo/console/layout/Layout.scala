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

package io.quckoo.console.layout

import diode.react.ModelProxy

import io.quckoo.console.ConsoleRoute
import io.quckoo.console.ConsoleRoute.{Dashboard, Registry, Scheduler}
import io.quckoo.console.components.Icons
import io.quckoo.console.core.ConsoleScope
import io.quckoo.console.layout.Navigation.NavigationItem
import io.quckoo.console.log.LogRecord

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.{Resolution, RouterCtl}
import japgolly.scalajs.react.vdom.html_<^._

import monix.reactive.Observable

/**
  * Created by alonsodomin on 08/05/2017.
  */
object Layout {

  final val MainMenu = List(
    NavigationItem(Icons.dashboard, "Dashboard", Dashboard),
    NavigationItem(Icons.book, "Registry", Registry),
    NavigationItem(Icons.clockO, "Scheduler", Scheduler)
  )

  case class Props(
      proxy: ModelProxy[ConsoleScope],
      logStream: Observable[LogRecord],
      routerCtl: RouterCtl[ConsoleRoute],
      resolution: Resolution[ConsoleRoute]
  )

  class Backend($ : BackendScope[Props, Unit]) {

    def render(props: Props) = {
      def navigation = props.proxy.wrap(_.passport.flatMap(_.principal)) {
        principal =>
          Navigation(MainMenu.head,
                     MainMenu,
                     props.routerCtl,
                     props.resolution.page,
                     principal)
      }

      <.div(navigation,
            props.resolution.render(),
            Footer(props.proxy, props.logStream))
    }

  }

  private[this] val component = ScalaComponent
    .builder[Props]("Layout")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[ConsoleScope], logStream: Observable[LogRecord])(
      routerCtl: RouterCtl[ConsoleRoute],
      resolution: Resolution[ConsoleRoute]) = {
    component(Props(proxy, logStream, routerCtl, resolution))
  }

}
