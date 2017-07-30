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

package io.quckoo.console.boot

import diode.ActionProcessor
import diode.react.ModelProxy

import io.quckoo.console.ConsoleRoute
import io.quckoo.console.core.{
  ConsoleCircuit,
  ConsoleScope,
  ErrorProcessor,
  EventLogProcessor,
  LoginProcessor
}
import io.quckoo.console.dashboard.DashboardLogger
import io.quckoo.console.registry.RegistryLogger
import io.quckoo.console.scheduler.SchedulerLogger
import io.quckoo.console.log.LogLevel

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom._
import japgolly.scalajs.react.vdom.Implicits._

/**
  * Created by alonsodomin on 14/05/2017.
  */
object ConsoleApp {

  case class Props(level: LogLevel, proxy: ModelProxy[ConsoleScope])
  case class State(router: Router[ConsoleRoute],
                   processors: List[ActionProcessor[ConsoleScope]])

  class Backend($ : BackendScope[Props, State]) {

    def initialise: Callback = {
      def connectProcessors(state: State): Callback =
        state.processors
          .map(p => Callback(ConsoleCircuit.addProcessor(p)))
          .foldLeft(Callback.empty)(_ >> _)

      $.state >>= connectProcessors
    }

    def dipose: Callback = {
      def disconnectProcessors(state: State): Callback =
        state.processors
          .map(p => Callback(ConsoleCircuit.removeProcessor(p)))
          .foldLeft(Callback.empty)(_ >> _)

      $.state >>= disconnectProcessors
    }

    def render(props: Props, state: State): VdomElement = {
      state.router()
    }

  }

  private[this] def initState(props: Props): State = {
    val logger = DashboardLogger orElse RegistryLogger orElse SchedulerLogger
    val logProcessor = new EventLogProcessor(props.level, logger)

    val routerConfig = ConsoleRouter.config(props.proxy, logProcessor.logStream)
    val (router, control) =
      Router.componentAndCtl(ConsoleRouter.baseUrl, routerConfig)

    val loginProcessor = new LoginProcessor(control)
    val errorProcessor = new ErrorProcessor

    val processors = List(logProcessor, loginProcessor, errorProcessor)
    State(router, processors)
  }

  private[this] val component = ScalaComponent
    .builder[Props]("ConsoleApp")
    .initialStateFromProps(initState)
    .renderBackend[Backend]
    .componentWillMount(_.backend.initialise)
    .componentWillUnmount(_.backend.dipose)
    .build

  def apply(level: LogLevel) = {
    ConsoleCircuit.wrap(identity(_)) { proxy =>
      component(Props(level, proxy))
    }
  }

}
