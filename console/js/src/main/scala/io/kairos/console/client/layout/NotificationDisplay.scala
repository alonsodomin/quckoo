package io.kairos.console.client.layout

import io.kairos.console.client.components._
import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Created by alonsodomin on 15/10/2015.
 */
object NotificationDisplay {
  import Notification._

  private[this] final val AlertClasses = Map(
    Level.Danger  -> ContextStyle.danger,
    Level.Warning -> ContextStyle.warning,
    Level.Info    -> ContextStyle.info,
    Level.Success -> ContextStyle.success
  )

  private[this] val component = ReactComponentB[Seq[Notification]]("AlertDisplay").
    stateless.
    noBackend.
    render_P(msgs =>
      <.div(msgs.map { msg =>
        val alertClass = AlertClasses(msg.level)
        Alert(alertClass, msg.renderer(msg.level).runNow())
      })
    ).build
  
  def apply(messages: Seq[Notification]) = component(messages)
  
}
