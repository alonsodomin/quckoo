package io.quckoo.console.client.boot

import io.quckoo.console.client.scheduler.SchedulerPage
import io.quckoo.console.client.layout._
import io.quckoo.console.client.registry.RegistryPage
import io.quckoo.console.client.security.LoginPage
import io.quckoo.console.client.{HomePage, SiteMap}
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
      LoginPage.Style,
      HomePage.Style,
      RegistryPage.Style,
      SchedulerPage.Style
    )
    GlobalRegistry.onRegistration(_.addToDocument)
  }

  @JSExport
  override def main(): Unit = {
    GlobalStyles.addToDocument()
    inlineStyles()

    val container = dom.document.getElementById("viewport")
    //dom.document.head.appendChild(GlobalStyle.contents)
    SiteMap.router().render(container)
  }
}
