package io.quckoo.console.dashboard

import diode.react.ModelProxy
import io.quckoo.console.core.{ClusterEvent, ClusterEventListener}
import io.quckoo.protocol.cluster.ClusterInfo
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}

import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.StyleSheet

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

  case class Props(proxy: ModelProxy[ClusterInfo])

  class Backend($: BackendScope[Props, Unit]) {

    def render(props: Props) = {
      val state = props.proxy()
      <.div(Style.container,
        <.div(^.`class` := "row",
          <.div(^.`class` := "col-sm-12",
            <.div(Style.sectionTitle, "Nodes"),
            <.div(^.`class` := "row",
              <.div(^.`class` := "col-sm-8", "Active"),
              <.div(^.`class` := "col-sm-4 text-right", state.nodeInfo.active)
            ),
            <.div(^.`class` := "row",
              <.div(^.`class` := "col-sm-8", "Unreachable"),
              <.div(^.`class` := "col-sm-4 text-right", state.nodeInfo.inactive)
            )
          )
        ),
        <.div(Style.topBuffer,
          <.div(^.`class` := "col-sm-12",
            <.div(Style.sectionTitle, "Workers"),
            <.div(^.`class` := "row",
              <.div(^.`class` := "col-sm-8", "Available"),
              <.div(^.`class` := "col-sm-4 text-right", state.workers)
            )
          )
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("ClusterView").
    stateless.
    renderBackend[Backend].
    build

  def apply(proxy: ModelProxy[ClusterInfo]) = component(Props(proxy))

}
