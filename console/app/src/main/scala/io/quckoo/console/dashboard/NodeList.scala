package io.quckoo.console.dashboard

import diode.react.ModelProxy
import io.quckoo.id.NodeId
import io.quckoo.net.{NodeStatus, QuckooNode}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 04/04/2016.
  */
object NodeList {

  def NodeRow[N <: QuckooNode] = ReactComponentB[N]("NodeRow").
    stateless.
    render_P { node =>
      val danger = node.status match {
        case NodeStatus.Unreachable => true
        case _                      => false
      }
      <.tr(danger ?= ^.color.red,
        <.td(node.id.toString()),
        <.td(node.location.host),
        <.td(node.status.toString())
      )
    } build

  case class Props[N <: QuckooNode](proxy: ModelProxy[Map[NodeId, N]])

  class Backend[N <: QuckooNode]($: BackendScope[Props[N], Unit]) {

    def render(props: Props[N]) = {
      val model = props.proxy()
      <.table(^.`class` := "table table-striped table-hover",
        <.thead(
          <.tr(
            <.th("ID"),
            <.th("Location"),
            <.th("Status")
          )
        ),
        <.tbody(
          model.values.map { node =>
            NodeRow.withKey(node.id.toString())(node)
          }
        )
      )
    }

  }

  private[this] def component[N <: QuckooNode] = ReactComponentB[Props[N]]("NodeList").
    stateless.
    renderBackend[Backend[N]].
    build

  def apply[N <: QuckooNode](proxy: ModelProxy[Map[NodeId, N]]) = component(Props(proxy))

}
