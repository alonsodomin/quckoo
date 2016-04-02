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

    val content = style(addClassName("container-fluid"))

    val leftPanel = style(
      addClassNames("col-md-4"),
      height(100 %%)
    )
  }

  case class Props(proxy: ModelProxy[ConsoleScope])

  class Backend($: BackendScope[Props, Unit]) {

    def render(props: Props) = {
      <.div(^.`class` := "container-fluid",
        <.div(^.`class` := "row-fluid",
          <.div(Style.leftPanel, props.proxy.connect(_.clusterState)(ClusterView(_))),
          <.div(^.`class` := "container-fluid", "Here goes the contents")
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
