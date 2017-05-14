/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.console.log

import cats.effect.IO

import io.quckoo.console.components._
import io.quckoo.console.layout.CssSettings

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import monix.execution.{Ack, Cancelable}
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{Observable, Observer}

import scala.concurrent.Future

/**
  * Created by alonsodomin on 06/05/2017.
  */
object LogDisplay {
  import CssSettings._

  object Style extends StyleSheet.Inline {
    import dsl._

    val messagesPanel = style(
      position.absolute,
      bottom(60 px),
      width(100 %%)
    )
  }

  case class Props(logStream: Observable[LogRecord], bufferSize: Int)
  case class State(buffer: List[LogRecord], visible: Boolean = false)

  class Backend($ : BackendScope[Props, State]) {
    private var subscription: Option[Cancelable] = Option.empty

    private[LogDisplay] def initialize(props: Props): Callback = Callback {
      val subscriptionRef = props.logStream.subscribe(new Observer[LogRecord] {
        override def onError(ex: Throwable): Unit = ()

        override def onComplete(): Unit = ()

        override def onNext(elem: LogRecord): Future[Ack] =
          appendRecord(elem).map(_ => Ack.Continue).unsafeToFuture()

      })
      subscription = Some(subscriptionRef)
    }

    private[LogDisplay] def dispose(): Callback = Callback {
      subscription.foreach(_.cancel())
      subscription = None
    }

    private[this] def appendRecord(record: LogRecord): IO[Unit] = {
      val callback = for {
        props <- $.props
        _     <- $.modState(st => st.copy(buffer = (record :: st.buffer).take(props.bufferSize)))
      } yield ()

      IO(callback.runNow())
    }

    private[this] def togglePanel: Callback =
      $.modState(st => st.copy(visible = !st.visible))

    private[this] def renderRecord(record: LogRecord): String = {
      s"${record.when} - [${record.level.entryName}] - ${record.message}"
    }

    private[this] def renderPanel(props: Props, state: State) = {
      val log = state.buffer.map(renderRecord).mkString("\n")

      Panel("Messages", addStyles = Seq(Style.messagesPanel))(Seq(<.pre(
        ^.border  := "solid 1px black",
        ^.height  := "20em",
        log
      )))
    }

    def render(props: Props, state: State): VdomElement = {
      <.div(
        <.div("Messages", ^.cursor.pointer, ^.onClick --> togglePanel),
        if (state.visible) renderPanel(props, state)
        else EmptyVdom
      )
    }

  }

  val component = ScalaComponent.builder[Props]("ConsoleLog")
    .initialState(State(List.empty))
    .renderBackend[Backend]
    .componentWillMount($ => $.backend.initialize($.props))
    .componentWillUnmount(_.backend.dispose())
    .build

  def apply(logStream: Observable[LogRecord], bufferSize: Int = 500) =
    component(Props(logStream, bufferSize))

}
