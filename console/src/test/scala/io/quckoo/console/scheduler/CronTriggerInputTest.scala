package io.quckoo.console.scheduler

import io.quckoo.Trigger

import japgolly.scalajs.react.test.ReactTestUtils

import org.scalatest.FunSuite

/**
  * Created by alonsodomin on 03/09/2016.
  */
class CronTriggerInputTest extends FunSuite {
  import CronTriggerInputState._
  import CronTriggerInputTestDsl._

  implicit val triggerEq = _root_.scalaz.Equal.equalA[Trigger.Cron]

  val invariants: dsl.Invariants = {
    var invars = dsl.emptyInvariant

    invars &= dsl.focus("Expression").obsAndState(_.expressionInput.value, _.inputExpr).assert.equal

    /*invars &= dsl.focus("Expected trigger").obsAndState(
      _.state.asCron.disjunction.toOption.map(Trigger.Cron),
      _.state.updatedTrigger
    ).assert.equal*/

    invars
  }

  def runPlan(plan: dsl.Plan): Report[String] = {
    val initialTrigger = Option.empty[Trigger.Cron]

    ReactTestUtils.withRenderedIntoDocument(CronTriggerInput(initialTrigger, onUpdate)) { comp =>
      def observe() = new CronTriggerInputObserver(comp.htmlDomZipper)

      val test = plan.
        addInvariants(invariants).
        withInitialState(State("")).
        test(Observer watch observe())

      test.runU
    }
  }

  test("input should perform validation of the cron expression") {
    val plan = Plan.action(
      blankForm +>
      setExpression("* * * * * *") +> emptyExpression.assert(false) +> hasError.assert.equal(false) >>
      setExpression("* *") +> emptyExpression.assert(false) +> hasError.assert.equal(true)
    )

    runPlan(plan).assert()
  }

}
