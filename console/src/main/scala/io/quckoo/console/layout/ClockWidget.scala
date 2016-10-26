/*
 * Copyright 2016 Antonio Alonso Dominguez
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

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.TimerSupport
import japgolly.scalajs.react.vdom.prefix_<^._

import org.threeten.bp.format.{DateTimeFormatter, FormatStyle}
import org.threeten.bp.{Clock, ZonedDateTime}

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 04/07/2016.
  */
object ClockWidget {

  private[this] final val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)

  final case class State(dateTime: ZonedDateTime)

  class Backend($ : BackendScope[Clock, State]) extends TimerSupport {

    protected[ClockWidget] def mounted(clock: Clock) =
      setInterval(tick(clock), 1 second)

    def tick(clock: Clock): Callback =
      $.modState(_.copy(dateTime = ZonedDateTime.now(clock)))

    def render(clock: Clock, state: State) = {
      <.span(formatter.format(state.dateTime))
    }

  }

  private[this] val component = ReactComponentB[Clock]("Clock")
    .initialState_P(clock => State(ZonedDateTime.now(clock)))
    .renderBackend[Backend]
    .componentDidMount($ => $.backend.mounted($.props))
    .configure(TimerSupport.install)
    .build

  def apply(implicit clock: Clock) = component.withKey("clock")(clock)

}
