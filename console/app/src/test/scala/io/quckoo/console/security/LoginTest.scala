package io.quckoo.console.security

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.test.ReactTestUtils
import monix.reactive.subjects.PublishSubject
import org.scalatest.FlatSpec
import utest.TestSuite
import utest.framework.{Test, Tree}

/**
  * Created by alonsodomin on 11/07/2016.
  */
object LoginTest extends TestSuite {
  import LoginTestState._
  import LoginTestDsl._

  val invariants: dsl.Invariants = {
    var invars = dsl.emptyInvariant

    invars &= dsl.focus("Input values match state values").
      obsAndState(obs => (obs.usernameInput.value, obs.passwordInput.value), st => (st.username, st.password)).
      assert.equal

    invars
  }

  val handlerSubject = PublishSubject[(String, String)]()

  def runPlan(plan: dsl.Plan): Report[String] = {
    val handler: (String, String) => Callback = (user, pass) => Callback {
      handlerSubject.onNext((user, pass))
      handlerSubject.onComplete()
    }

    ReactTestUtils.withRenderedIntoDocument(LoginForm(handler)) { comp =>
      def observe() = new LoginObserver(comp.htmlDomZipper)

      val test = plan.
        addInvariants(invariants).
        withInitialState(State("", "")).
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
