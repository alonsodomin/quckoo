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

import diode.data.Ready
import diode.react.ModelProxy

import io.quckoo.NodeId
import io.quckoo.net.QuckooNode
import io.quckoo.console.components.Table

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Created by alonsodomin on 04/04/2016.
  */
object NodeList {

  final val Columns = List('ID, 'Location, 'Status)

  case class Props[N <: QuckooNode](proxy: ModelProxy[Map[NodeId, N]])

  class Backend[N <: QuckooNode]($ : BackendScope[Props[N], Unit]) {

    def renderItem(props: Props[N])(nodeId: NodeId, node: N, column: Symbol): VdomNode =
      column match {
        case 'ID       => nodeId.toString
        case 'Location => node.location.host
        case 'Status   => node.status.toString
      }

    def render(props: Props[N]) = {
      val model = props.proxy()
      val items = model.values.map(node => node.id -> Ready(node)).toSeq

      Table(Columns, items, renderItem(props))
    }

  }

  private[this] def component[N <: QuckooNode] =
    ScalaComponent.build[Props[N]]("NodeList").stateless.renderBackend[Backend[N]].build

  def apply[N <: QuckooNode](proxy: ModelProxy[Map[NodeId, N]]) = component(Props(proxy))

}
