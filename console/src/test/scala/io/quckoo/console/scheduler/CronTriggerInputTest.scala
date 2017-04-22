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
import io.quckoo.console.test.ConsoleTestExports

import japgolly.scalajs.react.test.ReactTestUtils

import org.scalatest.FunSuite

/**
  * Created by alonsodomin on 03/09/2016.
  */
class CronTriggerInputTest extends FunSuite {
  import ConsoleTestExports._
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
      blankInput +>
      setExpression("* * * * * ?") +> emptyExpression.assert(false) +> hasError.assert.equal(false) >>
      setExpression("* *") +> emptyExpression.assert(false) +> hasError.assert.equal(true)
    )

    runPlan(plan).assert()
  }

}
