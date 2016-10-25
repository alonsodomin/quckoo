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
