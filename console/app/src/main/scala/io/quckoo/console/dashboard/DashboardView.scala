package io.quckoo.console.dashboard

import diode.react.ModelProxy
import io.quckoo.console.core.ConsoleScope
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.StyleSheet

/**
 * Created by alonsodomin on 13/10/2015.
 */
object DashboardView {

  object Style extends StyleSheet.Inline {
    import dsl._

    val leftPanel = style(
      addClassNames("col-md-2"),
      height(100 %%)
    )

    val content = style(addClassName("col-md-10"))
  }

  case class Props(proxy: ModelProxy[ConsoleScope])

  class Backend($: BackendScope[Props, Unit]) {

    def render(props: Props) = {
      <.div(^.`class` := "container",
        <.div(^.`class` := "row",
          <.div(Style.leftPanel, props.proxy.connect(_.clusterState)(ClusterView(_))),
          <.div(Style.content,
            props.proxy.connect(_.clusterState.masterNodes)(NodeList(_)),
            props.proxy.connect(_.clusterState.workerNodes)(NodeList(_))
          )
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("HomePage").
    stateless.
    renderBackend[Backend].
    build

  def apply(proxy: ModelProxy[ConsoleScope]) = component(Props(proxy))

}
