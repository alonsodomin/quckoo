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
import japgolly.scalajs.react.test.ReactTestUtils

import io.quckoo.console.test.ConsoleTestExports

import org.scalatest.FunSuite

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 26/02/2017.
  */
class FiniteDurationInputTest extends FunSuite {
  import ConsoleTestExports._
  import FiniteDurationInputTestDsl._

  val invariants: dsl.Invariants = {
    var invars = dsl.emptyInvariant

    invars &= dsl.focus("Length").obsAndState(_.lengthInput.value, _.length.map(_.toString).getOrElse("")).assert.equal
    invars &= dsl.focus("Unit").obsAndState(_.selectedUnitOpt.map(_.value), _.unit.map(_.toString)).assert.equal

    invars
  }

  def runPlan(plan: dsl.Plan): Report[String] = {
    ReactTestUtils.withRenderedIntoDocument(FiniteDurationInput("testFD", None, _ => Callback.empty)) { comp =>
      def observe() = new FiniteDurationInputObserver("testFD", comp.htmlDomZipper)

      val test = plan
        .addInvariants(invariants)
        .withInitialState(State())
        .test(Observer watch observe())

      test.runU
    }
  }

  test("FiniteDurationInput") {
    val plan = Plan.action(
      currentLength.assert.equal(None) +>
      validUnitOffer +>
      validationMsg.map(_.isDefined).assert.equal(false) +>
      setLength(324) >> currentLength.assert.equal(Some(324)) +>
      chooseUnit(MINUTES) >> selectedUnit.assert.equal(Some(MINUTES)) +>
      setLength(-1) >> validationMsg.map(_.isDefined).assert.equal(true) +>
      clearLength() +> currentLength.assert.equal(None)
    )

    runPlan(plan).assert()
  }

}
