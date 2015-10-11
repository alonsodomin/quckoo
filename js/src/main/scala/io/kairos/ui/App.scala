package io.kairos.ui

import japgolly.scalajs.react.React
import org.scalajs.dom

import scala.scalajs.js.annotation.JSExport

@JSExport
object App extends {
  import dom.document

  @JSExport
  def main(container: dom.html.Div): Unit = {
    React.render(LoginForm(), document.body)
  }

}
