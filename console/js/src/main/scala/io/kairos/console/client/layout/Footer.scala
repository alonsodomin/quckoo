package io.kairos.console.client.layout

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
      addClassName("footer")
    )

  }

  private[this] val component = ReactComponentB.static("Footer",
    <.footer(Style.content,
      <.p(^.paddingTop := "5px", "Built using scalajs/scalajs-react/scalacss")
    )
  ).buildU

  def apply() = component()

}
