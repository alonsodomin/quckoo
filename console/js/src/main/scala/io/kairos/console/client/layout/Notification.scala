package io.kairos.console.client.layout

import io.kairos.Fault
import io.kairos.console.client.components._
import io.kairos.console.client.layout.Notification.Level.Level
import japgolly.scalajs.react._

/**
 * Created by alonsodomin on 15/10/2015.
 */
object Notification {

  object Level extends Enumeration {
    type Level = Value
    val Danger, Warning, Info, Success = Value
  }

  private def level2Icon(level: Level.Level): Icon = level match {
    case Level.Danger  => Icons.exclamationCircle
    case Level.Warning => Icons.exclamationTriangle
    case Level.Info    => Icons.questionCircle
    case Level.Success => Icons.checkCircle
  }

  type ContentRenderer = Level.Level => CallbackTo[ReactNode]
  sealed trait ToContentRenderer {
    def apply(): ContentRenderer
  }
  object ToContentRenderer {
    import vdom.prefix_<^._

    import scala.language.implicitConversions

    implicit def fromVDom(elem: => ReactTagOf[_]): ToContentRenderer = () => level => CallbackTo(elem.render)

    implicit def fromRenderer(renderer: ContentRenderer): ToContentRenderer = () => renderer

    implicit def fromString(content: String): ToContentRenderer = () => level => CallbackTo {
      <.span(level2Icon(level), content)
    }

    implicit def fromFault(fault: Fault): ToContentRenderer =
      fromString(fault.toString)

    implicit def fromThrowable(throwable: Throwable): ToContentRenderer =
      fromString(s"${throwable.getClass.getName}: ${throwable.getMessage}")
  }

  def apply(level: Level, contentMagnet: ToContentRenderer): Notification =
    Notification(level, contentMagnet())

  def danger(magnet: ToContentRenderer): Notification =
    apply(Level.Danger, magnet)

  def warn(magnet: ToContentRenderer): Notification =
    apply(Level.Warning, magnet)

  def info(magnet: ToContentRenderer): Notification =
    apply(Level.Info, magnet)

  def success(magnet: ToContentRenderer): Notification =
    apply(Level.Success, magnet)

}

case class Notification(level: Level, renderer: Notification.ContentRenderer)
