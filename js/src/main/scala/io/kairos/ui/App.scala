package io.kairos.ui

import org.scalajs.dom

import scala.scalajs.js.annotation.JSExport

@JSExport
object App extends {

  @JSExport
  def main(container: dom.html.Div): Unit = {
    AppStyle.load()
    SiteMap.router().render(container)
  }

}
