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

package io.quckoo.console.scheduler

import diode.data._
import diode.react.ModelProxy
import diode.react.ReactPot._

import io.quckoo.{ExecutionPlan, JobId, JobSpec, PlanId}
import io.quckoo.console.components._
import io.quckoo.console.core.ConsoleCircuit.Implicits.consoleClock
import io.quckoo.console.core.{LoadExecutionPlans, LoadJobSpecs, UserScope}
import io.quckoo.protocol.scheduler.CancelExecutionPlan

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import org.threeten.bp.ZonedDateTime

import scalaz._
import scalaz.std.list._
import scalaz.syntax.applicative.{^ => _, _}
import scalaz.syntax.traverse._
import scalaz.syntax.show._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ExecutionPlanList {
  import ScalazReact._

  final val Columns = List(
    'Job,
    'Current,
    'Trigger,
    'Scheduled,
    'Execution,
    'Outcome,
    'Next
  )

  final val ActiveFilter: Table.Filter[PlanId, ExecutionPlan] =
    (id, plan) => !plan.finished && plan.nextExecutionTime.isDefined
  final val InactiveFilter: Table.Filter[PlanId, ExecutionPlan] =
    (id, plan) => !ActiveFilter(id, plan)

  final val Filters: Map[Symbol, Table.Filter[PlanId, ExecutionPlan]] = Map(
    'Active   -> ActiveFilter,
    'Inactive -> InactiveFilter
  )

  type OnCreate = Callback
  type OnClick = ExecutionPlan => Callback

  final case class Props(proxy: ModelProxy[UserScope], onCreate: OnCreate, onClick: OnClick)
  final case class State(selectedFilter: Option[Symbol] = None, selectedPlans: Set[PlanId] = Set.empty)

  class Backend($ : BackendScope[Props, State]) {

    private[ExecutionPlanList] def initialize(props: Props): Callback = {
      val model = props.proxy()

      def loadJobs: Callback =
        Callback.when(model.jobSpecs.size == 0)(props.proxy.dispatchCB(LoadJobSpecs))

      def loadPlans: Callback =
        Callback.when(model.executionPlans.size == 0)(props.proxy.dispatchCB(LoadExecutionPlans))

      loadJobs *> loadPlans
    }

    def selectedPlans: CallbackTo[Map[PlanId, Pot[ExecutionPlan]]] = for {
      plans     <- $.props.map(_.proxy().executionPlans)
      selection <- $.state.map(_.selectedPlans)
    } yield plans.get(selection)

    // Actions

    private[this] def activePlansSelected: CallbackTo[Seq[PlanId]] = {
      selectedPlans.map(_.toSeq.collect {
        case (id, Ready(plan)) if !plan.finished => id
      })
    }

    def cancelPlan(planId: PlanId): Callback =
      $.props.flatMap(_.proxy.dispatchCB(CancelExecutionPlan(planId)))

    def cancelAll: Callback = {
      def invokeCommand(planIds: List[PlanId]): Callback = for {
        proxy <- $.props.map(_.proxy)
        _     <- planIds.map(id => proxy.dispatchCB(CancelExecutionPlan(id))).sequence
      } yield ()

      activePlansSelected.map(_.toList) >>= invokeCommand
    }
    def cancelAllDisabled: Boolean = activePlansSelected.map(_.isEmpty).runNow()

    // Event handlers

    def onFilterClicked(filterType: Symbol): Callback =
      $.modState(_.copy(selectedFilter = Some(filterType)))

    def onPlanClicked(planId: PlanId): Callback = {
      def planClickedCB(plan: ExecutionPlan): Callback =
        $.props.flatMap(_.onClick(plan))

      def planIsNotReady: Callback =
        Callback.alert(s"Execution plan '$planId' is not ready yet.")

      $.props.map(_.proxy()).flatMap {
        _.executionPlans.get(planId).headOption.map(planClickedCB).getOrElse(planIsNotReady)
      }
    }

    def onPlanSelected(selection: Set[PlanId]): Callback =
      $.modState(_.copy(selectedPlans = selection))

    // Rendering

    def renderItem(model: UserScope)(planId: PlanId, plan: ExecutionPlan, column: Symbol): VdomNode = {
      def renderPlanName: VdomNode = {
        val jobSpec = model.jobSpecs.get(plan.jobId)
        <.a(^.onClick --> onPlanClicked(planId), jobSpec.render(_.displayName))
      }

      def renderDateTime(dateTime: ZonedDateTime): VdomNode =
        DateTimeDisplay(dateTime)

      column match {
        case 'Job       => renderPlanName
        case 'Current   => plan.currentTask.map(_.show).getOrElse(Cord.empty).toString()
        case 'Trigger   => plan.trigger.toString()
        case 'Scheduled => plan.lastScheduledTime.map(renderDateTime).orNull
        case 'Execution => plan.lastExecutionTime.map(renderDateTime).orNull
        case 'Outcome   => plan.lastOutcome.map(_.toString).getOrElse[String]("")
        case 'Next      => plan.nextExecutionTime.map(renderDateTime).orNull
      }
    }

    def renderRowActions(props: Props)(planId: PlanId, plan: ExecutionPlan) = {
      if (!plan.finished && plan.nextExecutionTime.isDefined) {
        Seq(
          Table.RowAction[PlanId, ExecutionPlan](
            NonEmptyList(Icons.stop, "Cancel"),
            cancelPlan)
        )
      } else Seq.empty
    }

    def render(props: Props, state: State) = {
      val model = props.proxy()
      <.div(
        ToolBar(
          Button(Button.Props(
            Some(props.onCreate),
            style = ContextStyle.primary
          ), Icons.plusSquare, "Execution Plan"),
          Button(Button.Props(
            Some(cancelAll),
            disabled = cancelAllDisabled
          ), Icons.stopCircle, "Cancel All")
        ),
        NavBar(
          NavBar
            .Props(List('All, 'Active, 'Inactive), 'All, onFilterClicked, style = NavStyle.pills),
          Table(
            Columns,
            model.executionPlans.seq,
            renderItem(model),
            key = Some("executionPlans"),
            actions = Some(renderRowActions(props)(_, _)),
            filter = state.selectedFilter.flatMap(Filters.get),
            onSelect = Some(onPlanSelected(_)),
            selected = state.selectedPlans
          )
        )
      )
    }

  }

  private[this] val component = ScalaComponent.builder[Props]("ExecutionPlanList")
    .initialState(State())
    .renderBackend[Backend]
    .componentDidMount($ => $.backend.initialize($.props))
    .build

  def apply(proxy: ModelProxy[UserScope], onCreate: OnCreate, onClick: OnClick) =
    component(Props(proxy, onCreate, onClick))

}
