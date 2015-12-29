package io.kairos.console.client.layout

import io.kairos.console.client.layout.Notification.Level.Level
import japgolly.scalajs.react.{CallbackTo, vdom}
import japgolly.scalajs.react.vdom.TagMod

/**
 * Created by alonsodomin on 15/10/2015.
 */
object Notification {

  object Level extends Enumeration {
    type Level = Value
    val Error, Warning, Info, Success = Value
  }

  type Content = CallbackTo[TagMod]
  sealed trait ContentMagnet {
    def apply(): Content
  }
  object ContentMagnet {
    import scala.language.implicitConversions

    implicit def fromVDom(elem: => TagMod): ContentMagnet = () => CallbackTo(elem)

    implicit def fromContent(content: Content): ContentMagnet = () => content

    implicit def fromString(content: String): ContentMagnet = () => CallbackTo {
      import vdom.prefix_<^._
      <.span(content)
    }

    implicit def fromThrowable(throwable: Throwable): ContentMagnet =
      fromString(s"${throwable.getClass.getName}: ${throwable.getMessage}")
  }

  def apply(level: Level, contentMagnet: ContentMagnet): Notification =
    Notification(level, contentMagnet())

  def error(magnet: ContentMagnet): Notification =
    apply(Level.Error, magnet)

  def warn(magnet: ContentMagnet): Notification =
    apply(Level.Warning, magnet)

  def info(magnet: ContentMagnet): Notification =
    apply(Level.Info, magnet)

  def success(magnet: ContentMagnet): Notification =
    apply(Level.Success, magnet)

}

case class Notification(level: Level, content: Notification.Content)
