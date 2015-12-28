package io.kairos.console.client.layout

import io.kairos.console.client.layout.Notification.Level.Level
import japgolly.scalajs.react
import japgolly.scalajs.react.{Callback, CallbackTo, ReactElement}

/**
 * Created by alonsodomin on 15/10/2015.
 */
object Notification {

  object Level extends Enumeration {
    type Level = Value
    val Error, Warning, Info, Success = Value
  }

  type Content = CallbackTo[ReactElement]

  object Implicits {
    import react.vdom.prefix_<^

    import scala.language.implicitConversions

    implicit def stringElement(content: String): Content = CallbackTo {
      import prefix_<^._
      <.span(content)
    }

    implicit def throwableElement(throwable: Throwable): Content = {
      def logException(): Callback = {
        val stackTrace = throwable.getStackTrace.map(_.toString).mkString("\n")
        val msg = s"${throwable.getClass.getName}: ${throwable.getMessage}\n$stackTrace"
        Callback.log(msg)
      }

      logException() >> stringElement(s"${throwable.getClass.getName}: ${throwable.getMessage}")
    }

  }

  def error(content: => ReactElement): Notification =
    Notification(Level.Error, CallbackTo(content))

  def error(content: Content): Notification =
    Notification(Level.Error, content)

  def warn(content: => ReactElement): Notification =
    Notification(Level.Warning, CallbackTo(content))

  def warn(content: Content): Notification =
    Notification(Level.Warning, content)

  def info(content: => ReactElement): Notification =
    Notification(Level.Info, CallbackTo(content))

  def info(content: Content): Notification =
    Notification(Level.Info, content)

  def success(content: => ReactElement): Notification =
    Notification(Level.Success, CallbackTo(content))

  def success(content: Content): Notification =
    Notification(Level.Success, content)

}

case class Notification(level: Level, content: Notification.Content)
