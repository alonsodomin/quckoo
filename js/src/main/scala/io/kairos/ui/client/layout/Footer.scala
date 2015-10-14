package io.kairos.ui.client.layout

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Created by alonsodomin on 13/10/2015.
 */
object Footer {

  private[this] val component = ReactComponentB.static("Footer",
    <.footer(^.textAlign.center,
      <.div(^.borderBottom := "1px solid grey", ^.padding := "0px"),
      <.p(^.paddingTop := "5px", "Built using scalajs/scalajs-react/scalacss")
    )
  ).buildU

  def apply() = component()

}
