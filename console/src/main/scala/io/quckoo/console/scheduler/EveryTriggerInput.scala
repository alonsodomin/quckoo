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

import io.quckoo.Trigger
import io.quckoo.console.components._
import io.quckoo.console.layout.lookAndFeel

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._

import scala.concurrent.duration.FiniteDuration

import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 08/04/2016.
  */
object EveryTriggerInput {
  @inline private def lnf = lookAndFeel

  case class Props(value: Option[Trigger.Every],
                   onUpdate: Option[Trigger.Every] => Callback,
                   readOnly: Boolean)
  case class State(freq: Option[FiniteDuration],
                   delay: Option[FiniteDuration],
                   delayEnabled: Boolean = false) {

    def this(trigger: Option[Trigger.Every]) =
      this(trigger.map(_.frequency), trigger.flatMap(_.startingIn))

  }

  implicit val propsReuse: Reusability[Props] =
    Reusability.caseClassExcept('onUpdate)
  implicit val stateReuse: Reusability[State] = Reusability.caseClass

  class Backend($ : BackendScope[Props, State]) {

    def propagateUpdate: Callback = {
      val value = $.state.map(st => st.freq.map(freq => (freq, st.delay)))
      value.flatMap {
        case Some((freq, delay)) =>
          val trigger = Trigger.Every(freq, delay)
          $.props.flatMap(_.onUpdate(Some(trigger)))

        case _ =>
          $.props.flatMap(_.onUpdate(None))
      }
    }

    def onFreqUpdate(value: Option[FiniteDuration]): Callback =
      $.modState(_.copy(freq = value), propagateUpdate)

    def onDelayUpdate(value: Option[FiniteDuration]): Callback =
      $.modState(_.copy(delay = value), propagateUpdate)

    def onToggleDelay: Callback =
      $.modState(
        st =>
          st.copy(
            delay = if (st.delayEnabled) None else st.delay,
            delayEnabled = !st.delayEnabled
        ),
        propagateUpdate
      )

    def render(props: Props, state: State) =
      <.div(
        <.div(
          lnf.formGroup,
          <.label(^.`class` := "col-sm-2 control-label", "Frequency"),
          <.div(
            ^.`class` := "col-sm-10",
            FiniteDurationInput("everyTrigger_freq", state.freq, onFreqUpdate, props.readOnly)
          )
        ),
        <.div(
          lnf.formGroup,
          <.label(^.`class` := "col-sm-2 control-label", "Delay"),
          <.div(
            ^.`class` := "col-sm-10",
            <.div(
              ^.`class` := "checkbox",
              <.label(
                <.input.checkbox(
                  ^.id := "enableDelay",
                  ^.onChange --> onToggleDelay,
                  ^.value := state.delayEnabled,
                  ^.readOnly := props.readOnly,
                  ^.disabled := props.readOnly
                ),
                "Enabled"
              )
            )
          )
        ),
        if (state.delayEnabled) {
          <.div(
            ^.`class` := "col-sm-offset-2",
            FiniteDurationInput("everyTrigger_delay", state.delay, onDelayUpdate, props.readOnly)
          )
        } else EmptyVdom
      )

  }

  val component = ScalaComponent
    .builder[Props]("EveryTriggerInput")
    .initialStateFromProps(props => new State(props.value))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(value: Option[Trigger.Every],
            onUpdate: Option[Trigger.Every] => Callback,
            readOnly: Boolean = false) =
    component(Props(value, onUpdate, readOnly))

}
