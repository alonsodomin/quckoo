package io.quckoo.console.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 05/07/2016.
  */
object NotificationContainer {

  val dataNotify = "data-notify".reactAttr

  private[this] val component = ReactComponentB[Unit]("NotificationContainer").
    stateless.
    render($ =>
      <.div(dataNotify := "container", ^.`class` := "col-xs-11 col-sm-3 alert alert-{0}", ^.role := "alert",
        <.button(^.`type` := "button", ^.aria.hidden := true, ^.`class` := "close", dataNotify := "dismiss", "x"),
        <.span(dataNotify := "icon"),
        <.span(dataNotify := "title", "{1}"),
        <.span(dataNotify := "message", "{2}"),
        <.div(^.`class` := "progress", dataNotify := "progressbar",
          <.div(^.`class` := "progress-bar progress-bar-{0}", ^.role := "progressbar",
            ^.aria.valuenow := "0", ^.aria.valuemin := "0", ^.aria.valuemax := "100"
            //^.style := "width: 0%;"
          )
        ),
        <.a(^.href := "{3}", ^.target := "{4}", dataNotify := "url")
      )
    ) build

  def apply() = component()

}
