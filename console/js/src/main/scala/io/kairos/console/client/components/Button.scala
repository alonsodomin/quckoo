package io.kairos.console.client.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 20/02/2016.
  */
object Button {
  case class Props(
      onClick: Option[Callback] = None,
      style: ContextStyle.Value = ContextStyle.default,
      addStyles: Seq[StyleA] = Seq()
  )

  val component = ReactComponentB[Props]("Button").
    renderPC { (_, p, c) =>
      val buttonType = if (p.onClick.isEmpty) "submit" else "button"
      <.button(lookAndFeel.buttonOpt(p.style), p.addStyles, ^.tpe := buttonType,
        p.onClick.isDefined ?= (^.onClick --> p.onClick.get), c
      )
    } build

  def apply() = component
  def apply(props: Props, children: ReactNode*) = component(props, children: _*)
}
