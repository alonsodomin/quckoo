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

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object TriggerSelect {

  final val Options = List('Immediate, 'After, 'Every, 'At, 'Cron)

  type Constructor = CoproductSelect.Constructor[Trigger]
  type Selector    = CoproductSelect.Selector[Trigger]
  type OnUpdate    = CoproductSelect.OnUpdate[Trigger]

  case class Props(value: Option[Trigger], onUpdate: OnUpdate, readOnly: Boolean = false)

  class Backend($ : BackendScope[Props, Unit]) {

    def selectComponent(props: Props): Selector = {
      case 'After =>
        (value, callback) =>
          AfterTriggerInput(value.map(_.asInstanceOf[Trigger.After]), callback, props.readOnly)
      case 'Every =>
        (value, callback) =>
          EveryTriggerInput(value.map(_.asInstanceOf[Trigger.Every]), callback, props.readOnly)
      case 'At =>
        (value, callback) =>
          AtTriggerInput(value.map(_.asInstanceOf[Trigger.At]), callback, props.readOnly)
      case 'Cron =>
        (value, callback) =>
          CronTriggerInput(value.map(_.asInstanceOf[Trigger.Cron]), callback, props.readOnly)
    }

    val selectInput = CoproductSelect[Trigger] {
      case Trigger.Immediate => 'Immediate
      case _: Trigger.After  => 'After
      case _: Trigger.Every  => 'Every
      case _: Trigger.At     => 'At
      case _: Trigger.Cron   => 'Cron
    }

    def render(props: Props) =
      selectInput(
        Options,
        selectComponent(props),
        props.value,
        Options.head,
        props.onUpdate,
        ^.id := "triggerType",
        ^.readOnly := props.readOnly,
        ^.disabled := props.readOnly
      )(
        Seq(<.label(^.`class` := "col-sm-2 control-label", ^.`for` := "triggerType", "Trigger"))
      )

  }

  val component = ScalaComponent
    .builder[Props]("TriggerSelect")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(value: Option[Trigger], onUpdate: OnUpdate, readOnly: Boolean = false) =
    component(Props(value, onUpdate, readOnly))

}
