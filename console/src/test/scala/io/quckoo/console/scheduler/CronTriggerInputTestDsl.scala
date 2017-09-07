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

import cron4s.Cron

import io.quckoo.Trigger
import io.quckoo.console.test.ConsoleTestExports

import monocle.macros.Lenses

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.test._

/**
  * Created by alonsodomin on 03/09/2016.
  */
object CronTriggerInputTestDsl {
  import ConsoleTestExports._

  @Lenses
  case class State(inputExpr: String, updatedTrigger: Option[Trigger.Cron] = None) {
    def asCron = Cron(inputExpr)
  }

  val dsl = Dsl[Unit, CronTriggerInputObserver, State]

  def blankInput = dsl.test("Blank input")(_.obs.expressionInput.value.isEmpty)

  def onUpdate(value: Option[Trigger.Cron]) = Callback {
    dsl
      .action("Update callback")
      .update(_.state)
      .updateState(_.copy(updatedTrigger = value))
  }

  def setExpression(value: String): dsl.Actions =
    dsl
      .action(s"Set expression: $value")(SimEvent.Change(value) simulate _.obs.expressionInput)
      .updateState(_.copy(inputExpr = value))

  def emptyExpression =
    dsl.focus("Expression").value(_.obs.expressionInput.value.isEmpty)

  def hasError = dsl.focus("Parse error").value(_.obs.parseError.isDefined)

}
