package io.quckoo.console.security

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.test.ReactTestUtils

import utest.TestSuite

/**
  * Created by alonsodomin on 11/07/2016.
  */
object LoginTest /*extends TestSuite {
  import LoginFormTestState._
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

  //val handlerSubject = PublishSubject[(String, String)]()

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

  override def tests = TestSuite {

    val plan = Plan.action(
      setUsername("admin")
        >> setPassword("password")
        >> submitForm()
    )

    runPlan(plan).assert()
  }

}
*/