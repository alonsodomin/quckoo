package io.kairos.ui.client.pages

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

    val content = style(
      textAlign.center,
      fontSize(30 px),
      minHeight(450 px),
      paddingTop(40 px)
    )
  }

  private[this] val component = ReactComponentB.static("HomePage",
    <.div(Style.content, "Kairos UI - Home")
  ).buildU

  def apply() = component()

}
