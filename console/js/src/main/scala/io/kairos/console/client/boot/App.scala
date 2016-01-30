package io.kairos.console.client.boot

import io.kairos.console.client.execution.ExecutionsPage
import io.kairos.console.client.layout._
import io.kairos.console.client.registry.RegistryPage
import io.kairos.console.client.security.LoginPage
import io.kairos.console.client.{HomePage, SiteMap}
import org.scalajs.dom

import scala.scalajs.js.annotation.JSExport
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry

@JSExport
object App extends {

  def inlineStyles() = {
    GlobalRegistry.register(
      Footer.Style,
      LoginPage.Style,
      HomePage.Style,
      RegistryPage.Style,
      ExecutionsPage.Style
    )
    GlobalRegistry.onRegistration(_.addToDocument)
  }

  @JSExport
  def main(container: dom.html.Div): Unit = {
    dom.document.head.appendChild(GlobalStyle.contents)
    inlineStyles()
    SiteMap.router().render(container)
  }

}
