package io.kairos.ui.client.execution

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object ExecutionsPage {

  private[this] val component = ReactComponentB.static("ExecutionsPage",
    <.div("Executions")
  ).buildU

  def apply() = component()

}
