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

package io.quckoo.console.dashboard

import diode.react.ModelProxy

import io.quckoo.console.model.ConsoleScope
import io.quckoo.console.layout.CssSettings._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._
import scalacss.StyleSheet

/**
  * Created by alonsodomin on 13/10/2015.
  */
object DashboardPage {

  object Style extends StyleSheet.Inline {
    import dsl._

    val leftPanel = style(
      addClassNames("col-md-2"),
      height(100 %%)
    )

    val content = style(addClassName("col-md-10"))
  }

  case class Props(proxy: ModelProxy[ConsoleScope])

  class Backend($ : BackendScope[Props, Unit]) {

    def render(props: Props) = {
      val clusterState = props.proxy.connect(_.clusterState)
      val masterNodes  = props.proxy.connect(_.clusterState.masterNodes)
      val workerNodes  = props.proxy.connect(_.clusterState.workerNodes)

      <.div(
        ^.`class` := "container",
        <.div(
          ^.`class` := "row",
          <.div(Style.leftPanel, clusterState(ClusterView(_))),
          <.div(
            Style.content,
            <.div(
              <.h3("Master nodes"),
              masterNodes(NodeList(_))
            ),
            <.div(
              <.h3("Worker nodes"),
              workerNodes(NodeList(_))
            )
          )
        )
      )
    }

  }

  private[this] val component =
    ScalaComponent
      .builder[Props]("DashboardView")
      .stateless
      .renderBackend[Backend]
      .build

  def apply(proxy: ModelProxy[ConsoleScope]) = component(Props(proxy))

}
