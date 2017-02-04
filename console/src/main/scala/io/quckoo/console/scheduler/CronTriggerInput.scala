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

import cron4s._

import io.quckoo.Trigger
import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 02/09/2016.
  */
object CronTriggerInput {

  private[this] val errorMessage =
    ReactComponentB[(String, InvalidCron)]("CronTriggerInput.ErrorMessage")
      .stateless
      .render_P { case (input, error) =>
        def showError(error: InvalidCron) = error match {
          case ParseFailed(msg, position) =>
            <.div(
              <.span(msg),
              <.br,
              Iterator.fill(position - 2)(NBSP).mkString + "^"
            )

          case ValidationError(fieldErrors) =>
            <.ul(fieldErrors.map(err => <.li(err.field.toString(), err.msg)).list.toList)
        }

        <.div(
          ^.id := "cronParseError",
          ^.color.red,
          showError(error)
        )
      } build

  case class Props(value: Option[Trigger.Cron], onUpdate: Option[Trigger.Cron] => Callback)
  case class State(inputExpr: Option[String], errorReason: Option[InvalidCron] = None)

  class Backend($ : BackendScope[Props, State]) {

    private[this] def doValidate(value: Option[String]) = {
      import scalaz._
      import Scalaz._

      def updateError(err: Option[InvalidCron]): Callback =
        $.modState(_.copy(errorReason = err)) >> $.props.flatMap(_.onUpdate(None))

      def invokeCallback(trigger: Option[Trigger.Cron]): Callback =
        updateError(None) >> $.props.flatMap(_.onUpdate(trigger))

      EitherT(value.map(Cron(_).disjunction))
        .map(Trigger.Cron)
        .cozip
        .fold(updateError, invokeCallback)
    }

    def onUpdate(value: Option[String]) =
      $.modState(_.copy(inputExpr = value)) >> doValidate(value)

    val expressionInput = Input[String]()

    def render(props: Props, state: State) = {
      <.div(
        ^.`class` := "form-group",
        <.label(^.`class` := "col-sm-2 control-label", "Expression"),
        <.div(
          ^.`class` := "col-sm-10",
          expressionInput(state.inputExpr, onUpdate _, ^.id := "cronTrigger")),
        <.div(
          ^.`class` := "col-sm-offset-2",
          state.inputExpr.zip(state.errorReason).map(p => errorMessage.withKey("cronError")(p))))
    }

  }

  val component = ReactComponentB[Props]("CronTriggerInput")
    .initialState_P(props => State(props.value.map(_.expr.toString)))
    .renderBackend[Backend]
    .build

  def apply(value: Option[Trigger.Cron], onUpdate: Option[Trigger.Cron] => Callback) =
    component(Props(value, onUpdate))

}
