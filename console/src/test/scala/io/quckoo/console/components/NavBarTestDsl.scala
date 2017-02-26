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

import monocle.macros.Lenses

import japgolly.scalajs.react.test._

/**
  * Created by alonsodomin on 29/07/2016.
  */
object NavBarTestDsl {
  import NavBarTestExports._
  import ReactTestUtils._

  @Lenses
  final case class State(currentItem: Option[Symbol] = None)

  val dsl = Dsl[Unit, NavBarObserver, State]

  def selectItem(item: Symbol): dsl.Actions =
    dsl.action(s"Select nav item $item")(Simulate click _.obs.navItems(item)).
      updateState(State.currentItem.set(Some(item)))

  val currentItem = dsl.focus("Current selected item").value(_.obs.activeNavItem)

}
