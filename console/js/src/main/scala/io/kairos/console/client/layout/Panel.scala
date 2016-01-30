package io.kairos.console.client.layout

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object Panel {

  type Props = (String, Seq[TagMod])

  private[this] val component = ReactComponentB[Props]("Panel").
    stateless.
    render_P { case (title, contents) =>
      <.div(^.`class` := "panel panel-default",
        <.div(^.`class` := "panel-heading", title),
        <.div(^.`class` := "panel-body", contents)
      )
    } build

  def apply(title: String, contents: TagMod*) =
    component((title, Seq(contents)))

}
