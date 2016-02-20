package io.kairos.console.client.layout

import io.kairos.console.client.components._
import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Created by alonsodomin on 15/10/2015.
 */
object NotificationDisplay {
  import Notification._

  private[this] val alertClasses: Map[Level.Level, (String, String)] = Map(
    Level.Danger  -> ("alert-danger", "exclamation-circle"),
    Level.Warning -> ("alert-warning", "exclamation-triangle"),
    Level.Info    -> ("alert-info", "question"),
    Level.Success -> ("alert-success", "check")
  )

  private[this] val component = ReactComponentB[Seq[Notification]]("AlertDisplay").
    stateless.
    noBackend.
    render_P(msgs =>
      <.div(msgs.map { msg =>
        val (alertClass, iconClass) = alertClasses(msg.level)
        <.div(^.`class` := s"alert $alertClass", ^.role := "alert", ^.padding := 5.px,
          Icons.icon(iconClass),
          msg.renderer.runNow()
        )
      })
    ).build
  
  def apply(messages: Seq[Notification]) = component(messages)
  
}
