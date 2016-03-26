package io.quckoo.console.components

import japgolly.scalajs.react.{ReactNode, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 20/02/2016.
  */
object Alert {
  case class Props(style: ContextStyle.Value)

  val component = ReactComponentB[Props]("Alert").
    renderPC { (_, p, c) =>
      <.div(lookAndFeel.alert(p.style), ^.role := "alert", ^.padding := 5.px, c)
    } build

  def apply() = component
  def apply(style: ContextStyle.Value, children: ReactNode*) = component(Props(style), children)
}
