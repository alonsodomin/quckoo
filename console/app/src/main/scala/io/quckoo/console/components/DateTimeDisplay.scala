package io.quckoo.console.components

import io.quckoo.time.DateTime

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 04/07/2016.
  */
object DateTimeDisplay {

  val DefaultPattern = "dddd, MMMM Do YYYY, h:mm:ss a"

  final case class Props(dateTime: Option[DateTime], pattern: String, useLocal: Boolean)

  private[this] val component = ReactComponentB[Props]("DateTimeDisplay").
    stateless.
    render_P { props =>
      val dt = {
        if (props.useLocal) props.dateTime.map(_.toLocal)
        else props.dateTime
      }
      <.span(dt.map(_.format(props.pattern)))
    } build

  def apply(dateTime: Option[DateTime], pattern: String = DefaultPattern, useLocal: Boolean = true) =
    component(Props(dateTime, pattern, useLocal))

}
