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

package io.quckoo.console.scheduler

import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.concurrent.duration.FiniteDuration

/**
  * Created by alonsodomin on 09/04/2016.
  */
object ExecutionTimeoutInput {

  case class Props(value: Option[FiniteDuration],
                   onUpdate: Option[FiniteDuration] => Callback,
                   readOnly: Boolean)
  case class State(enabled: Boolean = false)

  class Backend($ : BackendScope[Props, State]) {

    def onFlagUpdate: Callback =
      $.modState(st => st.copy(enabled = !st.enabled))

    def onValueUpdate(value: Option[FiniteDuration]): Callback =
      $.props.flatMap(_.onUpdate(value))

    def render(props: Props, state: State) =
      <.div(
        <.div(
          ^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2 control-label", "Timeout"),
          <.div(
            ^.`class` := "col-sm-10",
            <.div(
              ^.`class` := "checkbox",
              <.label(
                <.input.checkbox(
                  ^.id := "enableTimeout",
                  ^.value := state.enabled,
                  ^.onChange --> onFlagUpdate,
                  ^.readOnly := props.readOnly,
                  ^.disabled := props.readOnly
                ),
                "Enabled"
              )
            )
          )
        ),
        if (state.enabled) {
          <.div(
            ^.`class` := "col-sm-offset-2",
            FiniteDurationInput("timeout", props.value, onValueUpdate, props.readOnly)
          )
        } else EmptyVdom
      )

  }

  val component = ScalaComponent
    .builder[Props]("ExecutionTimeout")
    .initialState(State())
    .renderBackend[Backend]
    .build

  def apply(value: Option[FiniteDuration],
            onUpdate: Option[FiniteDuration] => Callback,
            readOnly: Boolean = false) =
    component(Props(value, onUpdate, readOnly))

}
