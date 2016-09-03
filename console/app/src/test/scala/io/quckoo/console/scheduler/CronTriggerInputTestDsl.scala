package io.quckoo.console.scheduler

import cron4s.Cron

import io.quckoo.Trigger

import monocle.macros.Lenses

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.test._

/**
  * Created by alonsodomin on 03/09/2016.
  */
object CronTriggerInputTestDsl {
  import CronTriggerInputState._

  @Lenses
  case class State(inputExpr: String, updatedTrigger: Option[Trigger.Cron] = None) {
    def asCron = Cron(inputExpr)
  }

  val dsl = Dsl[Unit, CronTriggerInputObserver, State]

  def blankForm = dsl.test("Blank form")(_.obs.expressionInput.value.isEmpty)

  def onUpdate(value: Option[Trigger.Cron]) = Callback {
    println("Setting trigger value: " + value)

    dsl.action("Update callback").update(_.state).updateState(_.copy(updatedTrigger = value))
  }

  def setExpression(value: String): dsl.Actions =
    dsl.action(s"Set expression: $value")(ChangeEventData(value) simulate _.obs.expressionInput).
      updateState(_.copy(inputExpr = value))

  def emptyExpression = dsl.focus("Expression").value(_.obs.expressionInput.value.isEmpty)

  def hasError = dsl.focus("Parse error").value(_.obs.parseError.isDefined)

}
