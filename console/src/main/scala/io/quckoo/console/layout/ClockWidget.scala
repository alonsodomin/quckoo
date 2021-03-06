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

package io.quckoo.console.layout

import java.time.{Clock, ZonedDateTime}
import java.time.format.DateTimeFormatter

import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.TimerSupport
import japgolly.scalajs.react.vdom.html_<^._

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 04/07/2016.
  */
object ClockWidget {

  private[this] final val Formatter = DateTimeFormatter.ofPattern(
    "E, MMM d, HH:mm:ss"
  )

  final case class State(current: ZonedDateTime)

  class Backend($ : BackendScope[Clock, State]) extends TimerSupport {

    protected[ClockWidget] def mounted: Callback =
      setInterval(tick(), 1 second)

    def tick(): Callback =
      $.props >>= updateCurrent

    def updateCurrent(clock: Clock): Callback =
      $.modState(_.copy(current = ZonedDateTime.now(clock)))

    def render(clock: Clock, state: State) =
      <.span(DateTimeDisplay(state.current.toLocalDateTime, Some(Formatter)))

  }

  private[this] val component = ScalaComponent
    .builder[Clock]("Clock")
    .initialStateFromProps(clock => State(ZonedDateTime.now(clock)))
    .renderBackend[Backend]
    .componentDidMount(_.backend.mounted)
    .configure(TimerSupport.install)
    .build

  def apply(implicit clock: Clock) = component.withKey("clock")(clock)

}
