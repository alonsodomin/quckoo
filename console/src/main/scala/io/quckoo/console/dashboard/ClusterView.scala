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

package io.quckoo.console.dashboard

import diode.AnyAction._
import diode.react.ModelProxy

import io.quckoo.NodeId
import io.quckoo.net.{NodeStatus, QuckooNode, QuckooState}
import io.quckoo.protocol.cluster.GetClusterStatus

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.StyleSheet

/**
  * Created by alonsodomin on 13/12/2015.
  */
object ClusterView {

  object Style extends StyleSheet.Inline {
    import dsl._

    val container = style(addClassName("well"))

    val sectionTitle = style(
      fontSize(16 px),
      fontWeight.bold
    )

    val topBuffer = style(
      addClassName("row"),
      marginTop(20 px)
    )

    object section {
      val title = style(
        fontSize(16 px),
        fontWeight.bold
      )
    }

    initInnerObjects(section.title)
  }

  case class Props(proxy: ModelProxy[QuckooState])

  class Backend($: BackendScope[Props, Unit]) {

    def mounted(props: Props) = {
      // We assume that if master node map is empty, then we haven't subscribed yet
      val unsubscribed = props.proxy().masterNodes.isEmpty
      Callback.when(unsubscribed)(props.proxy.dispatchCB(GetClusterStatus))
    }

    def render(props: Props) = {
      def countStatus(nodes: Iterable[(NodeId, QuckooNode)], status: NodeStatus): Int =
        nodes.map(_._2.status).count(_ == status)

      def activeMasters: Int =
        countStatus(props.proxy().masterNodes, NodeStatus.Active)

      def unreachableMasters: Int =
        countStatus(props.proxy().masterNodes, NodeStatus.Unreachable)

      def activeWorkers: Int =
        countStatus(props.proxy().workerNodes, NodeStatus.Active)

      def unreachableWorkers: Int =
        countStatus(props.proxy().workerNodes, NodeStatus.Unreachable)

      <.div(Style.container,
        <.div(^.`class` := "row",
          <.div(^.`class` := "col-sm-12",
            <.div(Style.sectionTitle, "Nodes"),
            <.div(^.`class` := "row",
              <.div(^.`class` := "col-sm-8", "Active"),
              <.div(^.`class` := "col-sm-4 text-right", activeMasters)
            ),
            <.div(^.`class` := "row",
              <.div(^.`class` := "col-sm-8", "Unreachable"),
              <.div(^.`class` := "col-sm-4 text-right", unreachableMasters)
            )
          )
        ),
        <.div(Style.topBuffer,
          <.div(^.`class` := "col-sm-12",
            <.div(Style.sectionTitle, "Workers"),
            <.div(^.`class` := "row",
              <.div(^.`class` := "col-sm-8", "Active"),
              <.div(^.`class` := "col-sm-4 text-right", activeWorkers)
            ),
            <.div(^.`class` := "row",
              <.div(^.`class` := "col-sm-8", "Unreachable"),
              <.div(^.`class` := "col-sm-4 text-right", unreachableWorkers)
            )
          )
        ),
        <.div(Style.topBuffer,
          <.div(^.`class` := "col-sm-12",
            <.div(Style.sectionTitle, "Tasks"),
            <.div(^.`class` := "row",
              <.div(^.`class` := "col-sm-8", "Pending"),
              <.div(^.`class` := "col-sm-4 text-right", props.proxy().metrics.pendingTasks)
            ),
            <.div(^.`class` := "row",
              <.div(^.`class` := "col-sm-8", "In Progress"),
              <.div(^.`class` := "col-sm-4 text-right", props.proxy().metrics.inProgressTasks)
            )
          )
        )
      )
    }

  }

  private[this] val component = ScalaComponent.build[Props]("ClusterView").
    stateless.
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[QuckooState]) = component(Props(proxy))

}
