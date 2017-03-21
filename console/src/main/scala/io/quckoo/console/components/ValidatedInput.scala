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

package io.quckoo.console.components

import io.quckoo.console.validation._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scalaz.{Success, Failure}
import scalaz.syntax.show._

object ValidatedInput {
  type OnUpdate[A] = Option[A] => Callback

  final case class Props[A](
      component: OnUpdate[A] => VdomNode,
      onUpdate: OnUpdate[A],
      validator: ValidatorCallback[A],
      label: Option[String]
  )
  final case class State[A](fieldState: ValidatedField[A])

  class Backend[A]($ : BackendScope[Props[A], State[A]]) {
    def onUpdate(props: Props[A])(newValue: Option[A]): Callback = {
      val validate = newValue.map { value =>
        props.validator.run(value).map {
          case Success(_)         => ValidatedField[A](Some(value))
          case Failure(violation) => ValidatedField[A](Some(value), Some(violation))
        }
      } getOrElse CallbackTo(ValidatedField[A](None))

      val updateState = validate
        .flatMap(newFieldState => $.modState(_.copy(fieldState = newFieldState)))
        .ret(newValue)

      updateState >>= props.onUpdate
    }

    def render(props: Props[A], state: State[A]) = {
      <.div(
        ^.classSet(
          "form-group"  -> true,
          "has-success" -> state.fieldState.valid,
          "has-error"   -> state.fieldState.invalid
        ),
        props.label.map(labelText => <.label(^.`class` := "control-label", labelText)).whenDefined,
        props.component(onUpdate(props)),
        state.fieldState.violation.map { violation =>
          <.span(^.`class` := "help-block", violation.shows)
        } whenDefined
      )
    }
  }

  def apply[A](validator: ValidatorCallback[A]) = new ValidatedInput[A](validator)

}

class ValidatedInput[A] private[components] (validator: ValidatorCallback[A]) {
  import ValidatedInput._

  private[this] val component = ScalaComponent.builder[Props[A]]("ValidatedInput")
    .initialState(State[A](ValidatedField()))
    .renderBackend[Backend[A]]
    .build

  def apply(onUpdate: OnUpdate[A], label: Option[String] = None)(
      factory: OnUpdate[A] => VdomNode) =
    component(Props(factory, onUpdate, validator, label))

}
