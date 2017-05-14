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

  case class Props(logStream: Observable[LogRecord])
  case class State(buffer: List[LogRecord])

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

    private[this] def appendRecord(record: LogRecord): IO[Unit] = IO {
      $.modState(st => st.copy(buffer = record :: st.buffer)).runNow()
    }

    def render(props: Props, state: State) = {
      val log = state.buffer.map(_.toString).mkString("\n")

      Panel("Messages")(Seq(<.pre(
        ^.border  := "solid 1px black",
        ^.height  := "20em",
        ^.padding := "2px 6px",
        log
      )))
    }

  }

  val component = ScalaComponent.builder[Props]("ConsoleLog")
    .initialState(State(List.empty))
    .renderBackend[Backend]
    .componentWillMount($ => $.backend.initialize($.props))
    .componentWillUnmount(_.backend.dispose())
    .build

  def apply(logStream: Observable[LogRecord]) =
    component(Props(logStream))

}
