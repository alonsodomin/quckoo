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

package io.quckoo.console.components

import java.util.concurrent.TimeUnit

import cats.instances.all._

import io.quckoo.validation.Validators._
import io.quckoo.console.validation._

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 08/04/2016.
  */
object FiniteDurationInput {

  val SupportedUnits = Seq(
    MILLISECONDS -> "Milliseconds",
    SECONDS      -> "Seconds",
    MINUTES      -> "Minutes",
    HOURS        -> "Hours",
    DAYS         -> "Days"
  )

  case class Props(id: String,
                   value: Option[FiniteDuration],
                   onUpdate: Option[FiniteDuration] => Callback,
                   readOnly: Boolean = false)
  case class State(length: Option[Long], unit: Option[TimeUnit]) {

    def this(duration: Option[FiniteDuration]) =
      this(duration.map(_.length), duration.map(_.unit))

  }

  implicit val propsReuse: Reusability[Props] = Reusability.by(_.value)
  implicit val stateReuse                     = Reusability.derive[State]

  class Backend($ : BackendScope[Props, State]) {

    def propagateUpdate: Callback = {
      val valuePair =
        $.state.map(st => st.length.flatMap(l => st.unit.map(u => (l, u))))

      val value = valuePair.map {
        case Some((length, unit)) =>
          Some(FiniteDuration(length, unit))
        case _ => None
      }

      value.flatMap(v => $.props.flatMap(_.onUpdate(v)))
    }

    def onLengthUpdate(value: Option[Long]): Callback =
      $.modState(_.copy(length = value), propagateUpdate)

    def onUnitUpdate(evt: ReactEventFromInput): Callback = {
      val value = {
        if (evt.target.value.isEmpty) None
        else Some(TimeUnit.valueOf(evt.target.value))
      }
      $.modState(_.copy(unit = value), propagateUpdate)
    }

    private[this] val _lengthInput = Input[Long]
    private[this] val LengthValidation =
      ValidatedInput[Long]((greaterThan(0L) or equalTo(0L)).callback)

    private[this] def lengthInput(props: Props, state: State)(onUpdate: Option[Long] => Callback) =
      _lengthInput(
        state.length,
        onUpdate,
        ^.id := s"${props.id}_length",
        ^.readOnly := props.readOnly
      )

    def render(props: Props, state: State) = {
      val id = props.id
      <.div(
        ^.`class` := "container-fluid",
        <.div(
          ^.`class` := "row",
          <.div(
            ^.`class` := "col-sm-4",
            LengthValidation(onLengthUpdate)(lengthInput(props, state))
          ),
          <.div(
            ^.`class` := "col-sm-6",
            <.select(
              ^.id := s"${id}_unit",
              ^.`class` := "form-control",
              ^.readOnly := props.readOnly,
              ^.disabled := props.readOnly,
              state.unit.map(u => ^.value := u.toString).whenDefined,
              ^.onChange ==> onUnitUpdate,
              <.option(^.key := s"${id}_none", ^.value := "", "Select a time unit..."),
              SupportedUnits.toVdomArray {
                case (u, text) =>
                  <.option(^.key := s"${id}_${u.name()}", ^.value := u.name(), text)
              }
            )
          )
        )
      )
    }

  }

  val component = ScalaComponent
    .builder[Props]("FiniteDurationInput")
    .initialStateFromProps(props => new State(props.value))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(id: String,
            value: Option[FiniteDuration],
            onUpdate: Option[FiniteDuration] => Callback,
            readOnly: Boolean = false) =
    component(Props(id, value, onUpdate, readOnly))

}
