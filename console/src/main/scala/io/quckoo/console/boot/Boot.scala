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

import io.quckoo.{Info, Logo}
import io.quckoo.console.core.{ConsoleCircuit, EventLogProcessor}
import io.quckoo.console.dashboard.{ClusterView, DashboardLogger, DashboardPage}
import io.quckoo.console.layout._
import io.quckoo.console.log
import io.quckoo.console.registry.RegistryPage
import io.quckoo.console.scheduler.SchedulerPage
import io.quckoo.console.security.LoginPage

import japgolly.scalajs.react.vdom.Implicits._

import org.scalajs.dom

import slogging.StrictLogging

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scalacss.internal.mutable.GlobalRegistry

object Boot extends JSApp with StrictLogging {
  import CssSettings._

  def inlineStyles() = {
    GlobalRegistry.register(
      LoginPage.Style,
      DashboardPage.Style,
      ClusterView.Style,
      RegistryPage.Style,
      SchedulerPage.Style
    )
    GlobalRegistry.onRegistration(_.addToDocument)
  }

  private[this] def setupUILogger(): Unit = {
    import slogging._

    LoggerConfig.factory = ConsoleLoggerFactory()
    LoggerConfig.level = LogLevel.DEBUG
  }

  private[this] def setupBackendLogger(): Unit = {
    import log.LogLevel

    val logProcessor = new EventLogProcessor(LogLevel.Info, DashboardLogger)
    ConsoleCircuit.addProcessor(logProcessor)
  }

  @JSExport
  override def main(): Unit = {
    setupUILogger()
    setupBackendLogger()

    GlobalStyles.addToDocument()
    inlineStyles()

    logger.info(s"Starting Quckoo Console ${Info.version}...\n" + Logo)

    val container = dom.document.getElementById("viewport")

    ConsoleCircuit.wrap(identity(_))(p => App(p)).renderIntoDOM(container)
  }
}
