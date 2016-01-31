package io.kairos.console.client.layout

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 31/01/2016.
  */
object Button {

  private[this] val component = ReactComponentB[String]("Button").
    stateless.
    noBackend.
    render_P { text =>
      <.div(^.`class` := "col-sm-offset-2",
        <.button(^.`class` := "btn btn-default", text)
      )
    } build

  def submit(text: String = "Submit") = component(text)

}
