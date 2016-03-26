package io.quckoo.console.components

import japgolly.scalajs.react.{ReactNode, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 20/02/2016.
  */
object Panel {
  case class Props(heading: String, style: ContextStyle.Value = ContextStyle.default)

  val component = ReactComponentB[Props]("Panel").
    renderPC { (_, p, c) =>
      <.div(lookAndFeel.panelOpt(p.style),
        <.div(lookAndFeel.panelHeading, p.heading),
        <.div(lookAndFeel.panelBody, c)
      )
    } build

  def apply() = component
  def apply(props: Props, children: ReactNode*) = component(props, children: _*)
}
