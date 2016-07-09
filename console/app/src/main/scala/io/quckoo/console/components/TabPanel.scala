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

import japgolly.scalajs.react._

/**
  * Created by alonsodomin on 28/05/2016.
  */
object TabPanel {

  final case class Props(items: Map[String, ReactNode], initial: String)
  final case class State(selected: Option[String] = None)

  class Backend($: BackendScope[Props, State]) {

    def tabClicked(title: String): Callback =
      $.modState(_.copy(selected = Some(title)))

    def render(props: Props, state: State) = {
      val currentTab = state.selected.getOrElse(props.initial)
      TabBar(
        TabBar.Props(props.items.keys.toSeq, props.initial, tabClicked),
        props.items(currentTab)
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("TabPanel").
    initialState(State()).
    renderBackend[Backend].
    build

  def apply(tabItems: (String, ReactNode)*) = {
    val initial = tabItems.head._1
    component(Props(tabItems.toMap, initial))
  }

}
