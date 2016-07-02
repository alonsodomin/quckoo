package io.quckoo.console.components

import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactNode}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 28/05/2016.
  */
object TabPanel {

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

  final case class Props(items: Map[String, ReactNode], initial: String)
  final case class State(selected: Option[String] = None)

  class Backend($: BackendScope[Props, State]) {

    def tabClicked(title: String): Callback =
      $.modState(_.copy(selected = Some(title)))

    def render(props: Props, state: State) = {
      val currentTab = state.selected.getOrElse(props.initial)
      <.div(
        <.ul(^.`class` := "nav nav-tabs",
          props.items.keys.map { title =>
            TabItem.withKey(s"tab-$title")(
              TabItemProps(title, currentTab == title, tabClicked)
            )
          }
        ),
        <.div(props.items(currentTab))
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("TabPanel").
    initialState(State()).
    renderBackend[Backend].
    build

  def apply(tabItems: (String, ReactNode)*) = {
    val initial = tabItems.head._1
    component(Props(tabItems.toMap, initial))
  }

}
