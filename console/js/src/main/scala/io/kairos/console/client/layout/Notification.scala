package io.kairos.console.client.layout

import io.kairos.Fault
import io.kairos.console.client.layout.Notification.Level.Level
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.{CallbackTo, vdom}

/**
 * Created by alonsodomin on 15/10/2015.
 */
object Notification {

  object Level extends Enumeration {
    type Level = Value
    val Error, Warning, Info, Success = Value
  }

  type ContentRenderer = CallbackTo[TagMod]
  sealed trait ToContentRenderer {
    def apply(): ContentRenderer
  }
  object ToContentRenderer {
    import scala.language.implicitConversions

    implicit def fromVDom(elem: => TagMod): ToContentRenderer = () => CallbackTo(elem)

    implicit def fromRenderer(renderer: ContentRenderer): ToContentRenderer = () => renderer

    implicit def fromString(content: String): ToContentRenderer = () => CallbackTo {
      import vdom.prefix_<^._
      <.span(content)
    }

    implicit def fromFault(fault: Fault): ToContentRenderer =
      fromString(fault.toString)

    implicit def fromThrowable(throwable: Throwable): ToContentRenderer =
      fromString(s"${throwable.getClass.getName}: ${throwable.getMessage}")
  }

  def apply(level: Level, contentMagnet: ToContentRenderer): Notification =
    Notification(level, contentMagnet())

  def error(magnet: ToContentRenderer): Notification =
    apply(Level.Error, magnet)

  def warn(magnet: ToContentRenderer): Notification =
    apply(Level.Warning, magnet)

  def info(magnet: ToContentRenderer): Notification =
    apply(Level.Info, magnet)

  def success(magnet: ToContentRenderer): Notification =
    apply(Level.Success, magnet)

}

case class Notification(level: Level, renderer: Notification.ContentRenderer)
