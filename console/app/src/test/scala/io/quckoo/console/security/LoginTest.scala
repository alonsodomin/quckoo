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
class LoginTest extends TestSuite {
  import LoginTestState._
  import LoginTestDsl._

  val invariants: dsl.Invariants = {
    val invars = dsl.emptyInvariant

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
