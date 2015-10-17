package io.kairos.ui.client.boot

import io.kairos.ui.client.layout.Footer
import io.kairos.ui.client.security.LoginPage
import io.kairos.ui.client.{HomePage, SiteMap}
import org.scalajs.dom

import scala.scalajs.js.annotation.JSExport
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry

@JSExport
object App extends {

  def styles() = {
    GlobalRegistry.register(
      Footer.Style,
      LoginPage.Style,
      HomePage.Style
    )
    GlobalRegistry.onRegistration(_.addToDocument)
  }

  @JSExport
  def main(container: dom.html.Div): Unit = {
    styles()
    SiteMap.router().render(container)
  }

}
