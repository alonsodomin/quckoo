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

import io.quckoo.console.test.ConsoleTestExports

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.test.ReactTestUtils

import org.scalatest.FunSuite

/**
  * Created by alonsodomin on 25/02/2017.
  */
// TODO Ignored because Sizzle is failing to select the DOM element
class TextAreaTestIgnore /** extends FunSuite **/ {
  import ConsoleTestExports._
  import TextAreaTestDsl._

  val invariants: dsl.Invariants = {
    var invars = dsl.emptyInvariant

    invars &= dsl.focus("Text").obsAndState(_.textArea.value, _.text.getOrElse("")).assert.equal

    invars
  }

  def runPlan(plan: dsl.Plan): Report[String] = {
    val textAreaDef = TextArea(Some("hello"), onUpdate, ^.id := "testTextArea", ^.name := "myTextArea")

    ReactTestUtils.withRenderedIntoDocument(textAreaDef) { comp =>
      def observe() = new TextAreaObserver(comp.htmlDomZipper)

      val test = plan
        .addInvariants(invariants)
        .withInitialState(TextAreaState())
        .test(Observer watch observe())

      test.runU
    }
  }

  /*test("TextArea should collect the text of the input") {
    val plan = Plan.action(
      blankInput +>
      setText("foo") +> emptyText.assert(false) >>
      setText("") +> emptyText.assert(true)
    )

    runPlan(plan).assert()
  }*/

}
