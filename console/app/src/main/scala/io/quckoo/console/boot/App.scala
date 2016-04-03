package io.quckoo.console.boot

import io.quckoo.console.SiteMap
import io.quckoo.console.core.ConsoleCircuit
import io.quckoo.console.dashboard.{ClusterView, DashboardView}
import io.quckoo.console.layout._
import io.quckoo.console.registry.RegistryPageView
import io.quckoo.console.scheduler.SchedulerPageView
import io.quckoo.console.security.LoginPageView
import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry

@JSExport
object App extends JSApp {

  def inlineStyles() = {
    GlobalRegistry.register(
      LoginPageView.Style,
      DashboardView.Style,
      ClusterView.Style,
      RegistryPageView.Style,
      SchedulerPageView.Style
    )
    GlobalRegistry.onRegistration(_.addToDocument)
  }

  @JSExport
  override def main(): Unit = {
    GlobalStyles.addToDocument()
    inlineStyles()

    val container = dom.document.getElementById("viewport")
    //dom.document.head.appendChild(GlobalStyle.contents)
    ConsoleCircuit.connect(identity(_))(p => SiteMap(p)).render(container)
  }
}
