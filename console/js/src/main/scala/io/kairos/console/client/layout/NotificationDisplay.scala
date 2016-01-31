package io.kairos.console.client.layout

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Created by alonsodomin on 15/10/2015.
 */
object NotificationDisplay {
  import Notification._
  import Level.Level

  private[this] def bgClassForLevel(level: Level): String = level match {
    case Level.Error   => "alert-danger"
    case Level.Warning => "alert-warning"
    case Level.Info    => "alert-info"
    case Level.Success => "alert-success"
  }

  private[this] def iconClassForLevel(level: Level): String = level match {
    case Level.Error   => "fa-exclamation-circle"
    case Level.Warning => "fa-exclamation-triangle"
    case Level.Info    => "fa-question"
    case Level.Success => "fa-check"
  }

  private[this] val component = ReactComponentB[Seq[Notification]]("GlobalMessages").
    stateless.
    noBackend.
    render_P(msgs =>
      <.div(msgs.map { msg =>
        <.div(^.`class` := s"alert ${bgClassForLevel(msg.level)}", ^.padding := 5.px,
          <.i(^.`class` := s"fa ${iconClassForLevel(msg.level)}", ^.paddingRight := 5.px),
          msg.renderer.runNow()
        )
      })
    ).build
  
  def apply(messages: Seq[Notification]) = component(messages)
  
}
