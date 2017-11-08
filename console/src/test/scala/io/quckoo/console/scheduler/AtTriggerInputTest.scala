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

import java.time._

import io.quckoo.Trigger
import io.quckoo.console.test.ConsoleTestExports

import japgolly.scalajs.react.test.ReactTestUtils

import org.scalatest.FunSuite

object AtTriggerInputTest {
  final val TestClock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
}

class AtTriggerInputTest extends FunSuite {
  import ConsoleTestExports._
  import AtTriggerInputTestDsl._
  import AtTriggerInputTest._

  val invariants: dsl.Invariants = {
    var invars = dsl.emptyInvariant

    invars &= dsl
      .focus("Date")
      .obsAndState(_.dateInput.value, _.date.map(_.toString).getOrElse(""))
      .assert
      .equal

    invars &= dsl
      .focus("Time")
      .obsAndState(_.timeInput.value, _.time.map(_.toString).getOrElse(""))
      .assert
      .equal

    invars
  }

  def runPlan(plan: dsl.Plan): Report[String] = {
    val initialTrigger = Option.empty[Trigger.At]

    ReactTestUtils.withRenderedIntoDocument(AtTriggerInput(initialTrigger, onUpdate)) { comp =>
      def observe() = new AtTriggerInputObserver(comp.htmlDomZipper)

      val test = plan
        .addInvariants(invariants)
        .withInitialState(State(None, None))
        .test(Observer watch observe())

      test.runU
    }
  }

  test("input should update date and time") {
    val newDate = LocalDate.now(TestClock).plusDays(25)
    val newTime = LocalTime.now(TestClock).minusMinutes(15)

    val plan = Plan.action(
      noDate.assert(true) +> noTime.assert(true) +>
        setDate(newDate) +> noDate.assert(false) +> noTime.assert(true) >>
        setTime(newTime) +> noDate.assert(false) +> noTime.assert(false)
    )

    runPlan(plan).assert()
  }
}
