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

import cats.data.EitherT
import cats.implicits._

import cron4s.{Error => CronError, _}

import io.quckoo.Trigger
import io.quckoo.console.components._
import io.quckoo.util._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Created by alonsodomin on 02/09/2016.
  */
object CronTriggerInput {

  private[this] val errorMessage =
    ScalaComponent
      .builder[(String, CronError)]("CronTriggerInput.ErrorMessage")
      .stateless
      .render_P {
        case (input, error) =>
          def showError(error: CronError) = error match {
            case ParseFailed(expected, found, position) =>
              <.div(
                <.span(s"Expected $expected but found $found"),
                <.br,
                Iterator.fill(position - 2)(NBSP).mkString + "^"
              )

            case InvalidCron(errors) =>
              <.ul(errors.toList.zipWithIndex.map {
                case (err, idx) =>
                  <.li(^.key := s"cron-err-$idx", err match {
                    case InvalidField(field, msg)     => s"$field: $msg"
                    case InvalidFieldCombination(msg) => msg
                  })
              }.toVdomArray)
          }

          <.div(
            ^.id := "cronParseError",
            ^.color.red,
          showError(error)
        )
    } build

  case class Props(value: Option[Trigger.Cron],
                   onUpdate: Option[Trigger.Cron] => Callback,
                   readOnly: Boolean)
  case class State(inputExpr: Option[String], errorReason: Option[CronError] = None)

  class Backend($ : BackendScope[Props, State]) {

    private[this] def doValidate(value: Option[String]) = {

      def updateError(err: Option[CronError]): Callback =
        $.modState(_.copy(errorReason = err)) >> $.props.flatMap(_.onUpdate(None))

      def invokeCallback(trigger: Option[Trigger.Cron]): Callback =
        updateError(None) >> $.props.flatMap(_.onUpdate(trigger))

      EitherT(value.map(Cron(_)))
        .map(Trigger.Cron(_))
        .cozip
        .fold(updateError, invokeCallback)
    }

    def onUpdate(value: Option[String]): Callback =
      $.modState(_.copy(inputExpr = value)) >> doValidate(value)

    private[this] val ExpressionInput = Input[String]

    def render(props: Props, state: State) =
      <.div(
        ^.`class` := "form-group",
        <.label(^.`class` := "col-sm-2 control-label", "Expression"),
        <.div(
          ^.`class` := "col-sm-10",
          ExpressionInput(
            state.inputExpr,
            onUpdate _,
            ^.id := "cronTrigger",
            ^.readOnly := props.readOnly
          )
        ),
        <.div(
          ^.`class` := "col-sm-offset-2",
          (state.inputExpr, state.errorReason)
            .mapN((input, error) => errorMessage((input, error)))
            .whenDefined
        )
      )

  }

  val component = ScalaComponent
    .builder[Props]("CronTriggerInput")
    .initialStateFromProps(props => State(props.value.map(_.expr.toString)))
    .renderBackend[Backend]
    .build

  def apply(value: Option[Trigger.Cron],
            onUpdate: Option[Trigger.Cron] => Callback,
            readOnly: Boolean = false) =
    component(Props(value, onUpdate, readOnly))

}
