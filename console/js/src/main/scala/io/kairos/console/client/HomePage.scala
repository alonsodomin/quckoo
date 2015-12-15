package io.kairos.console.client

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 13/10/2015.
 */
object HomePage {

  object Style extends StyleSheet.Inline {
    import dsl._

    val content = style(addClassName("container-fluid"))

    val leftPanel = style(
      addClassNames("col-md-4"),
      height(100 %%)
    )
  }

  private[this] val component = ReactComponentB.static("HomePage",
    <.div(^.`class` := "container-fluid",
      <.div(^.`class` := "row-fluid",
        <.div(Style.leftPanel, ClusterView()),
        <.div(^.`class` := "container-fluid", "Here goes the contents")
      )
    )
  ).buildU

  def apply() = component()

}
