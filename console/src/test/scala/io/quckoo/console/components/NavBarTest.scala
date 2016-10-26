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

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.test.ReactTestUtils

import org.scalatest.FunSuite

/**
  * Created by alonsodomin on 29/07/2016.
  */
class NavBarTest extends FunSuite {
  import NavBarTestState._
  import NavBarTestDsl._

  val invariants: dsl.Invariants = {
    dsl.focus("Selected item").obsAndState(_.activeNavItem, _.currentItem).assert.equal
  }

  def runPlan(plan: dsl.Plan): Report[String] = {
    val items = Seq("First", "Last")

    ReactTestUtils.withRenderedIntoDocument(NavBar(NavBar.Props(items, "First", _ => Callback.empty))) { comp =>
      def observe() = new NavBarObserver(comp.htmlDomZipper)

      val test = plan.
        addInvariants(invariants).
        withInitialState(State(Some("First"))).
        test(Observer watch observe())

      test.runU
    }
  }

  test("should switch between the different tabs") {
    val plan = Plan.action(
      selectItem("Last") +> currentItem.assert.equal(Some("Last")) >>
      selectItem("First") +> currentItem.assert.equal(Some("First"))
    )

    runPlan(plan).assert()
  }

}
