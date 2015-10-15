package io.kairos.ui.client

import io.kairos.ui.client.pages.{HomePage, LoginPage, WelcomePage}
import org.scalajs.dom

import scala.scalajs.js.annotation.JSExport
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry

@JSExport
object App extends {

  def styles() = {
    GlobalRegistry.register(
      WelcomePage.Style,
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
