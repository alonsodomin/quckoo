package io.quckoo.console.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 09/07/2016.
  */
object TabBar {

  private[this] final case class TabItemProps(
    title: String,
    selected: Boolean,
    onClick: String => Callback
  )

  private[this] val TabItem = ReactComponentB[TabItemProps]("TabItem").
    stateless.
    render_P { case TabItemProps(title, selected, onClick) =>
      <.li(^.role := "presentation", selected ?= (^.`class` := "active"),
        <.a(^.onClick --> onClick(title), title)
      )
    } build

  private[this] val TabBody = ReactComponentB[PropsChildren]("TabBody").
    stateless.
    render_P { children =>
      <.div(children)
    } build

  final case class Props(
    items: Seq[String],
    initial: String,
    onClick: String => Callback,
    style: NavStyle.Value = NavStyle.tabs
  )
  final case class State(selected: Option[String] = None)

  class Backend($: BackendScope[Props, State]) {

    def tabClicked(props: Props)(title: String): Callback =
      $.modState(_.copy(selected = Some(title))).flatMap(_ => props.onClick(title))

    def render(props: Props, state: State) = {
      val currentTab = state.selected.getOrElse(props.initial)
      <.div(
        <.ul(lookAndFeel.nav(props.style),
          props.items.map { title =>
            TabItem.withKey(s"tab-item-$title")(
              TabItemProps(title, currentTab == title, tabClicked(props))
            )
          }
        ),
        TabBody.withKey("tab-panel-body")($.propsChildren.runNow())
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("TabBar").
    initialState(State()).
    renderBackend[Backend].
    build

  def apply(props: Props, children: ReactNode*) =
    component(props, children: _*)

}
