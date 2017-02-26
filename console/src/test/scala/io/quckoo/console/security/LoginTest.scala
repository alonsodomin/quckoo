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

package io.quckoo.console.security

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.test.ReactTestUtils

import org.scalatest.FunSuite

/**
  * Created by alonsodomin on 11/07/2016.
  */
class LoginTest extends FunSuite {
  import LoginFormTestExports._
  import LoginTestDsl._

  val invariants: dsl.Invariants = {
    var invars = dsl.emptyInvariant

    invars &=
      dsl.focus("Username").obsAndState(_.usernameInput.value, _.username).assert.equal &
      dsl.focus("Password").obsAndState(_.passwordInput.value, _.password).assert.equal

    invars &= dsl.test("Can not submit until full form is filled"){ i =>
      (i.obs.emptyUsername || i.obs.emptyPassword) == !i.obs.canSubmit
    }

    invars
  }

  def runPlan(plan: dsl.Plan): Report[String] = {

    ReactTestUtils.withRenderedIntoDocument(LoginForm((_, _) => Callback.empty)) { comp =>
      def observe() = new LoginObserver(comp.htmlDomZipper)

      val test = plan.
        addInvariants(invariants).
        withInitialState(LoginState("", "")).
        test(Observer watch observe())

      test.runU
    }
  }

  test("should be able to submit user credentials") {
    val plan = Plan.action(
      setUsername("admin")
        >> setPassword("password")
        >> submitForm()
    )

    runPlan(plan).assert()
  }

}
