/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.console.components

import diode.data.{Pot, Ready}
import diode.react.ReactPot._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

import scalaz.NonEmptyList

/**
  * Created by alonsodomin on 02/07/2016.
  */
object Table {

  type OnSelect[Id] = Set[Id] => Callback

  type RowCallback[Id]             = Id => Callback
  type RowCellRender[Id, Item]     = (Id, Item, Symbol) => ReactNode
  type RowActionsFactory[Id, Item] = (Id, Item) => Seq[RowAction[Id, Item]]

  type ItemSeq[Id, Item] = Traversable[(Id, Pot[Item])]

  type Filter[Id, Item] = (Id, Item) => Boolean
  def NoFilter[Id, Item]: Filter[Id, Item] = (_, _) => true

  final case class RowAction[Id, Item](children: NonEmptyList[ReactNode], execute: RowCallback[Id])

  private[this] final case class RowProps[Id, Item](
      rowId: Id,
      columns: List[Symbol],
      item: Pot[Item],
      render: RowCellRender[Id, Item],
      onClick: Option[RowCallback[Id]],
      allowSelect: Boolean,
      selected: Boolean,
      toggleSelected: RowCallback[Id],
      actions: Option[RowActionsFactory[Id, Item]]
  )

  private[this] val HeaderCell =
    ReactComponentB[Symbol]("HeaderCell").stateless.render_P(title => <.th(title.name)).build

  private[this] val BodyCell =
    ReactComponentB[ReactNode]("BodyCell").stateless.render_P(node => <.td(node)).build

  private[this] case class CheckboxCellProps(
      id: String,
      checked: Boolean,
      action: Callback,
      header: Boolean = false
  )
  private[this] val CheckboxCell =
    ReactComponentB[CheckboxCellProps]("CheckboxCell").stateless.render_P {
      case CheckboxCellProps(id, selected, action, header) =>
        val cb = <.input.checkbox(
          ^.id := id,
          ^.checked := selected,
          ^.onChange --> action
        )

        if (header) <.th(cb)
        else <.td(cb)
    } build

  private[this] type ActionsCellProps[Id, Item] = (Id, Item, RowActionsFactory[Id, Item])
  private[this] def actionsCell[Id, Item] =
    ReactComponentB[ActionsCellProps[Id, Item]]("ActionsCell").stateless.render_P {
      case (id, item, factory) =>
        <.td(
          factory(id, item).zipWithIndex.map {
            case (action, idx) =>
              Button().withKey(s"action-$id-$idx")(
                Button.Props(Some(action.execute(id))),
                action.children.list.toList: _*)
          }
        )
    } build

  private[this] def row[Id, Item] =
    ReactComponentB[RowProps[Id, Item]]("Row").stateless.render_P { props =>
      <.tr(props.selected ?= (^.`class` := "info"),
        props.onClick.map(callback => ^.onClick --> callback(props.rowId)),
        props.item.renderFailed { ex =>
          <.td(^.colSpan := props.columns.size, Notification.danger(ex))
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
              val checkboxCell = CheckboxCell.withKey(s"select-${props.rowId}")(
                CheckboxCellProps(
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
          actions.map(actCell => cells :+ actCell).getOrElse[List[ReactElement]](cells)
        }
      )
    } build

  final case class Props[Id, Item](
      headers: List[Symbol],
      items: ItemSeq[Id, Item],
      render: RowCellRender[Id, Item],
      onRowClick: Option[RowCallback[Id]],
      onSelect: Option[OnSelect[Id]],
      actions: Option[RowActionsFactory[Id, Item]],
      filter: Option[Filter[Id, Item]],
      selected: Set[Id],
      style: Set[TableStyle.Value]
  )
  final case class State[Id](selected: Set[Id])

  class Backend[Id, Item]($ : BackendScope[Props[Id, Item], State[Id]]) {

    private[this] def propagateSelection: Callback =
      $.props.flatMap(p => $.state.flatMap(s => p.onSelect.map(_(s.selected)).getOrElse(Callback.empty)))

    def visibleItems(props: Props[Id, Item]): ItemSeq[Id, Item] =
      props.filter map { f =>
        props.items.filter {
          case (id, Ready(item)) => f(id, item)
          case _                 => true
        }
      } getOrElse props.items

    def allSelected(props: Props[Id, Item], state: State[Id]): Boolean =
      visibleItems(props).map(_._1).forall(state.selected.contains)

    def toggleSelectAll(props: Props[Id, Item]): Callback = {
      def updateState(state: State[Id]): State[Id] = {
        if (allSelected(props, state)) state.copy(selected = Set.empty[Id])
        else state.copy(selected = props.items.map(_._1).toSet)
      }

      $.modState(updateState, propagateSelection)
    }

    def toggleSelectItem(props: Props[Id, Item])(id: Id): Callback = {
      def updateState(state: State[Id]): State[Id] = {
        val newSet = {
          if (state.selected.contains(id))
            state.selected - id
          else state.selected + id
        }
        state.copy(selected = newSet)
      }

      $.modState(updateState, propagateSelection)
    }

    def render(props: Props[Id, Item], state: State[Id]) = {
      val headers: List[ReactElement] = {
        val actionsHeader: ReactElement = <.th("Actions")
        val columns = props.headers.map { title =>
          HeaderCell.withKey(s"$title-header")(title)
        }

        val selectableColumns = {
          if (props.onSelect.isDefined) {
            val selectAllCheckbox = CheckboxCell.withKey("select-all")(
              CheckboxCellProps(
                "selectAll",
                allSelected(props, state),
                toggleSelectAll(props),
                header = true)
            )
            selectAllCheckbox :: columns
          } else columns
        }

        if (props.actions.nonEmpty) {
          selectableColumns :+ actionsHeader
        } else selectableColumns
      }

      val userStyles = props.onRowClick
        .map(_ => props.style + TableStyle.hover)
        .getOrElse(props.style)
        .map(lookAndFeel.table.apply(_))
        .toSeq
      val style = if (userStyles.isEmpty) Seq(lookAndFeel.table.base) else userStyles

      <.table(style, <.thead(<.tr(headers)), <.tbody(visibleItems(props).map {
        case (id, item) =>
          row[Id, Item].withKey(s"row-$id")(
            RowProps(
              id,
              props.headers,
              item,
              props.render,
              props.onRowClick,
              props.onSelect.isDefined,
              state.selected.contains(id),
              toggleSelectItem(props),
              props.actions)
          )
      }))
    }

  }

  private[components] def component[Id, Item] =
    ReactComponentB[Props[Id, Item]]("Table")
      .initialState_P(props => State(props.selected))
      .renderBackend[Backend[Id, Item]]
      .build

  def apply[Id, Item](headers: List[Symbol],
                      items: ItemSeq[Id, Item],
                      render: RowCellRender[Id, Item],
                      onRowClick: Option[RowCallback[Id]] = None,
                      onSelect: Option[OnSelect[Id]] = None,
                      actions: Option[RowActionsFactory[Id, Item]] = None,
                      filter: Option[Filter[Id, Item]] = None,
                      selected: Set[Id] = Set.empty[Id],
                      style: Set[TableStyle.Value] = Set.empty[TableStyle.Value],
                      key: Option[String] = None) = {
    val baseComp = component[Id, Item]
    val instance = key.map(k => baseComp.withKey(k)).getOrElse(baseComp)
    instance.apply(Props(headers, items, render, onRowClick, onSelect, actions, filter, selected, style))
  }

}
