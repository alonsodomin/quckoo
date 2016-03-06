package io.kairos.console.client.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.Listenable
import japgolly.scalajs.react.vdom.prefix_<^._
import monifu.reactive.Observable
import monifu.concurrent.Implicits.globalScheduler

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 20/02/2016.
  */
object Button {
  case class Props(
      onClick: Option[Callback] = None,
      disabled: Boolean = false,
      style: ContextStyle.Value = ContextStyle.default,
      addStyles: Seq[StyleA] = Seq()
  )

  case class State(enabled: Boolean = true)

  val component = ReactComponentB[Props]("Button").
    initialState(State()).
    renderPCS { (_, p, children, state) =>
      val buttonType = if (p.onClick.isEmpty) "submit" else "button"
      <.button(lookAndFeel.buttonOpt(p.style), p.addStyles, ^.tpe := buttonType,
        p.onClick.map(handler => ^.onClick --> handler),
        p.disabled ?= (^.disabled := true),
        children
      )
    }.
    build

  def apply() = component
  def apply(props: Props, children: ReactNode*) = component(props, children: _*)
}
