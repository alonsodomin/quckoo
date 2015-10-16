package io.kairos.ui.client.layout

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 13/10/2015.
 */
object Footer {

  object Style extends StyleSheet.Inline {
    import dsl._

    val content = style(
      position.absolute,
      left(0 px),
      bottom(0 px),
      height(70 px),
      width(100 %%),
      textAlign.center
    )

  }

  private[this] val component = ReactComponentB.static("Footer",
    <.footer(Style.content,
      <.div(^.borderBottom := "1px solid grey", ^.padding := "0px"),
      <.p(^.paddingTop := "5px", "Built using scalajs/scalajs-react/scalacss")
    )
  ).buildU

  def apply() = component()

}
