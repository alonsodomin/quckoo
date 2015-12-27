package io.kairos.console.client.layout

import io.kairos.console.client.layout.Notification.Level.Level
import japgolly.scalajs.react
import japgolly.scalajs.react.CallbackTo

/**
 * Created by alonsodomin on 15/10/2015.
 */
object Notification {

  type Content = CallbackTo[react.ReactElement]

  object Level extends Enumeration {
    type Level = Value
    val Error, Warning, Info, Success = Value
  }

  object Implicits {
    import react.vdom.prefix_<^

    import scala.language.implicitConversions

    implicit def stringElement(content: String): Content = CallbackTo {
      import prefix_<^._
      <.span(content)
    }

    implicit def throwableElement(throwable: Throwable): Content =
      stringElement(throwable.getMessage)

  }

  def error(content: Content): Notification =
    Notification(Level.Error, content)

  def warn(content: Content): Notification =
    Notification(Level.Warning, content)

  def info(content: Content): Notification =
    Notification(Level.Info, content)

  def success(content: Content): Notification =
    Notification(Level.Success, content)

}

case class Notification(level: Level, content: Notification.Content)
