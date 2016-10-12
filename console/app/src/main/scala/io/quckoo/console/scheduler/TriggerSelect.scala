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

package io.quckoo.console.scheduler

import io.quckoo.Trigger

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 08/04/2016.
  */
object TriggerSelect {

  object TriggerType extends Enumeration {
    val Immediate, After, Every, At, Cron = Value
  }

  private[this] val TriggerOption =
    ReactComponentB[TriggerType.Value]("TriggerOption").stateless.render_P { triggerType =>
      <.option(^.value := triggerType.id, triggerType.toString())
    } build

  private[this] def triggerOptions: Seq[ReactElement] =
    TriggerType.values.toSeq.map(t => TriggerOption.withKey(t.id)(t))

  case class Props(value: Option[Trigger], onUpdate: Option[Trigger] => Callback)
  case class State(selected: Option[TriggerType.Value], value: Option[Trigger]) {

    def this(trigger: Option[Trigger]) = {
      this(trigger.map {
        case Trigger.Immediate => TriggerType.Immediate
        case _: Trigger.After  => TriggerType.After
        case _: Trigger.Every  => TriggerType.Every
        case _: Trigger.At     => TriggerType.At
        case _: Trigger.Cron   => TriggerType.Cron
      }, trigger)
    }

  }

  class Backend($ : BackendScope[Props, State]) {

    def propagateChange: Callback =
      $.state.flatMap(st => $.props.flatMap(_.onUpdate(st.value)))

    def onUpdateSelection(evt: ReactEventI): Callback = {
      def refreshNewSelection: CallbackTo[Option[TriggerType.Value]] = {
        val newSelection = {
          if (evt.target.value.isEmpty) None
          else Some(evt.target.value.toInt)
        } map (TriggerType(_))

        $.modState(_.copy(selected = newSelection)).ret(newSelection)
      }

      def updateTriggerValue(selection: Option[TriggerType.Value]): Callback = {
        val trigger = selection.flatMap {
          case TriggerType.Immediate => Some(Trigger.Immediate)
          case _                     => None
        }
        $.modState(_.copy(value = trigger), propagateChange)
      }

      refreshNewSelection >>= updateTriggerValue
    }

    def onUpdateValue[T <: Trigger](value: Option[T]): Callback =
      $.modState(_.copy(value = value), propagateChange)

    def render(props: Props, state: State) = {
      def triggerInput[T <: Trigger](
          constructor: (Option[T], Option[T] => Callback) => ReactNode): ReactNode =
        constructor(state.value.map(_.asInstanceOf[T]), onUpdateValue)

      <.div(
        <.div(
          ^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2 control-label", ^.`for` := "trigger", "Trigger"),
          <.div(
            ^.`class` := "col-sm-10",
            <.select(
              ^.`class` := "form-control",
              ^.id := "trigger",
              state.selected.map(v => ^.value := v.id),
              ^.onChange ==> onUpdateSelection,
              triggerOptions
            )
          )
        ),
        state.selected.flatMap {
          case TriggerType.After => Some(triggerInput(AfterTriggerInput.apply))
          case TriggerType.Every => Some(triggerInput(EveryTriggerInput.apply))
          case TriggerType.At    => Some(triggerInput(AtTriggerInput.apply))
          case TriggerType.Cron  => Some(triggerInput(CronTriggerInput.apply))
          case _                 => None
        }
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("TriggerSelect")
    .initialState_P(props => new State(props.value))
    .renderBackend[Backend]
    .build

  def apply(value: Option[Trigger], onUpdate: Option[Trigger] => Callback) =
    component(Props(value, onUpdate))

}
