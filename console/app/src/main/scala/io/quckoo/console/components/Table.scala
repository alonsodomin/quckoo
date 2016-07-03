package io.quckoo.console.components

import diode.data.Pot
import diode.react.ReactPot._

import io.quckoo.fault.ExceptionThrown

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalaz.NonEmptyList

/**
  * Created by alonsodomin on 02/07/2016.
  */
object Table {

  type RowCallback[Id] = Id => Callback
  type RowCellRender[Id, Item] = (Id, Item, String) => ReactNode
  type RowActionsFactory[Id, Item] = (Id, Item) => Seq[RowAction[Id, Item]]

  type ItemSeq[Id, Item] = Traversable[(Id, Pot[Item])]

  final case class RowAction[Id, Item](children: NonEmptyList[ReactNode], execute: RowCallback[Id])

  private[this] final case class RowProps[Id, Item](
    rowId: Id,
    columns: List[String],
    item: Pot[Item],
    render: RowCellRender[Id, Item],
    allowSelect: Boolean,
    selected: Boolean,
    toggleSelected: RowCallback[Id],
    actions: Option[RowActionsFactory[Id, Item]]
  )

  private[this] val HeaderCell = ReactComponentB[String]("HeaderCell").
    stateless.
    render_P(title => <.th(title)).
    build

  private[this] val BodyCell = ReactComponentB[ReactNode]("BodyCell").
    stateless.
    render_P(node => <.td(node)).
    build

  private[this] type CheckboxCellProps = (String, Boolean, Callback)
  private[this] val CheckboxCell = ReactComponentB[CheckboxCellProps]("CheckboxCell").
    stateless.
    render_P { case (id, selected, action) =>
      <.td(<.input.checkbox(
        ^.id := id,
        ^.checked := selected,
        ^.onChange --> action
      ))
    } build

  private[this] type ActionsCellProps[Id, Item] = (Id, Item, RowActionsFactory[Id, Item])
  private[this] def actionsCell[Id, Item] = ReactComponentB[ActionsCellProps[Id, Item]]("ActionsCell").
    stateless.
    render_P { case (id, item, factory) =>
      <.td(
        factory(id, item).zipWithIndex.map { case (action, idx) =>
          Button().withKey(s"action-$id-$idx")(Button.Props(
            Some(action.execute(id))),
            action.children.list.toList: _*
          )
        }
      )
    } build

  private[this] def row[Id, Item] = ReactComponentB[RowProps[Id, Item]]("Row").
    stateless.
    render_P { props =>
      <.tr(props.selected ?= (^.`class` := "info"),
        props.item.renderFailed { ex =>
          <.td(^.colSpan := props.columns.size, Notification.danger(ExceptionThrown(ex)))
        },
        props.item.renderPending { _ =>
          <.td(^.colSpan := props.columns.size, "Loading ...")
        },
        props.item.render { item =>
          val cells: List[ReactElement] = {
            val columns = props.columns.map { column =>
              BodyCell.withKey(s"$column-data-${props.rowId}")(
                props.render(props.rowId, item, column)
              )
            }

            if (props.allowSelect) {
              val checkboxCell = CheckboxCell.
                withKey(s"select-${props.rowId}")((
                  s"select-item-${props.rowId}",
                  props.selected,
                  props.toggleSelected(props.rowId)
                ))

              checkboxCell :: columns
            } else columns
          }

          val actions: Option[ReactElement] = props.actions.map { actions =>
            actionsCell[Id, Item].withKey(s"actions-${props.rowId}")(
              (props.rowId, item, actions)
            )
          }
          actions.map(actCell => cells :+ actCell).
            getOrElse[List[ReactElement]](cells)
        }
      )
    } build

  final case class Props[Id, Item](
    headers: List[String],
    items: ItemSeq[Id, Item],
    render: RowCellRender[Id, Item],
    allowSelect: Boolean,
    actions: Option[RowActionsFactory[Id, Item]]
  )
  final case class State[Id](selected: Set[Id], allSelected: Boolean = false)

  class Backend[Id, Item]($: BackendScope[Props[Id, Item], State[Id]]) {

    def toggleSelectAll(props: Props[Id, Item]): Callback = {
      $.modState { state =>
        if (state.allSelected) state.copy(selected = Set.empty[Id], allSelected = false)
        else state.copy(allSelected = true)
      }
    }

    def toggleSelectItem(props: Props[Id, Item])(id: Id): Callback = {
      $.modState { state =>
        val newSet = {
          if (state.selected.contains(id))
            state.selected - id
          else state.selected + id
        }
        state.copy(selected = newSet)
      }
    }

    def render(props: Props[Id, Item], state: State[Id]) = {
      val headers: List[ReactElement] = {
        val actionsHeader: ReactElement = <.th("Actions")
        val columns = props.headers.map { title =>
          HeaderCell.withKey(s"$title-header")(title)
        }

        val selectableColumns = {
          if (props.allowSelect) {
            val selectAllCheckbox = CheckboxCell.
              withKey("select-all")(
                ("selectAll", state.allSelected, toggleSelectAll(props))
              )
            selectAllCheckbox :: columns
          } else columns
        }

        if (props.actions.nonEmpty) {
          selectableColumns :+ actionsHeader
        } else selectableColumns
      }

      <.table(^.`class` := "table table-striped",
        <.thead(<.tr(headers)),
        <.tbody(props.items.map { case (id, item) =>
          row[Id, Item].withKey(s"row-$id")(
            RowProps(id, props.headers, item, props.render,
              props.allowSelect,
              state.selected.contains(id) || state.allSelected,
              toggleSelectItem(props),
              props.actions
            )
          )
        })
      )
    }

  }

  private[this] def component[Id, Item] = ReactComponentB[Props[Id, Item]]("Table").
    initialState(State(Set.empty[Id])).
    renderBackend[Backend[Id, Item]].
    build

  def apply[Id, Item](headers: List[String],
                      items: ItemSeq[Id, Item],
                      render: RowCellRender[Id, Item],
                      allowSelect: Boolean = false,
                      actions: Option[RowActionsFactory[Id, Item]] = None) =
    component.apply(Props(headers, items, render, allowSelect, actions))

}
