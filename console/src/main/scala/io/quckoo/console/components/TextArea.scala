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

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._

object TextArea {

  type OnUpdate = Option[String] => Callback

  case class Props(text: Option[String], onUpdate: OnUpdate, attrs: Seq[TagMod])
  case class State(value: Option[String])

  implicit val propsReuse: Reusability[Props] =
    Reusability.caseClassExcept('onUpdate, 'attrs)
  implicit val stateReuse: Reusability[State] = Reusability.caseClass[State]

  class Backend($ : BackendScope[Props, State]) {

    private[this] def propagateUpdate: Callback =
      $.state.flatMap(st => $.props.flatMap(_.onUpdate(st.value)))

    def onUpdate(evt: ReactEventFromInput): Callback = {
      val newValue = {
        if (evt.target.value.isEmpty) None
        else Some(evt.target.value)
      }

      $.modState(_.copy(value = newValue), propagateUpdate)
    }

    def render(props: Props, state: State) = {
      <.textarea(^.`class` := "form-control",
                 ^.onChange ==> onUpdate,
                 ^.onBlur ==> onUpdate,
                 state.value.map(v => ^.value := v).whenDefined,
                 props.attrs.toTagMod)
    }

  }

  val component = ScalaComponent
    .builder[Props]("TextArea")
    .initialStateFromProps(props => State(props.text))
    .renderBackend[Backend]
    .build

  def apply(value: Option[String], onUpdate: OnUpdate, attrs: TagMod*) =
    component(Props(value, onUpdate, attrs))

}
