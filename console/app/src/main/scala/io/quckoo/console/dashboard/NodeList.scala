package io.quckoo.console.dashboard

import diode.react.ModelProxy
import io.quckoo.id.NodeId
import io.quckoo.net.QuckooNode

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 04/04/2016.
  */
object NodeList {

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
            <.tr(
              <.td(node.id.toString()),
              <.td(node.location.host),
              <.td(node.status.toString())
            )
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
