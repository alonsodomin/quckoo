package io.quckoo.console.components

import io.quckoo.console.ConsoleTestState

import monocle.macros.Lenses

import japgolly.scalajs.react.test._

/**
  * Created by alonsodomin on 29/07/2016.
  */
object NavBarTestDsl {
  import ConsoleTestState._
  import ReactTestUtils._

  @Lenses
  final case class State(currentItem: Option[String] = None)

  val dsl = Dsl[Unit, NavBarObserver, State]

  def selectItem(item: String): dsl.Actions =
    dsl.action(s"Select nav item $item")(Simulate click _.obs.navItems(item)).
      updateState(State.currentItem.set(Some(item)))

  val currentItem = dsl.focus("Current selected item").value(_.obs.activeNavItem)

}
