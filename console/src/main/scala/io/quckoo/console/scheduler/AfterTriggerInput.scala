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
import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._

import scala.concurrent.duration.FiniteDuration

/**
  * Created by alonsodomin on 08/04/2016.
  */
object AfterTriggerInput {

  case class Props(value: Option[Trigger.After], onUpdate: Option[Trigger.After] => Callback, readOnly: Boolean)
  implicit val propsReuse = Reusability.by[Props, Option[Trigger.After]](_.value)

  class Backend($ : BackendScope[Props, Unit]) {

    def onUpdate(value: Option[FiniteDuration]) =
      $.props.flatMap(_.onUpdate(value.map(Trigger.After(_))))

    def render(props: Props): VdomElement =
      <.div(
        ^.`class` := "form-group",
        <.label(^.`class` := "col-sm-2 control-label", "Delay"),
        <.div(
          ^.`class` := "col-sm-10",
          FiniteDurationInput("afterTrigger", props.value.map(_.delay), onUpdate, props.readOnly)))

  }

  val component = ScalaComponent.build[Props]("AfterTriggerInput").stateless
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(value: Option[Trigger.After], onUpdate: Option[Trigger.After] => Callback, readOnly: Boolean = false) =
    component(Props(value, onUpdate, readOnly))

}
