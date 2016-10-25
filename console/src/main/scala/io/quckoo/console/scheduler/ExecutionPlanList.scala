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

import diode.react.ModelProxy
import diode.react.ReactPot._

import io.quckoo.ExecutionPlan
import io.quckoo.console.components._
import io.quckoo.console.core.{LoadExecutionPlans, LoadJobSpecs, UserScope}
import io.quckoo.id.PlanId
import io.quckoo.protocol.scheduler.CancelExecutionPlan
import io.quckoo.time.implicits.systemClock

import japgolly.scalajs.react._

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ExecutionPlanList {

  final val Columns = List(
    "Job",
    "Current task",
    "Trigger",
    "Last Scheduled",
    "Last Execution",
    "Last Outcome",
    "Next Execution"
  )

  final val AllFilter: Table.Filter[PlanId, ExecutionPlan] =
    (id, plan) => true
  final val ActiveFilter: Table.Filter[PlanId, ExecutionPlan] =
    (id, plan) => plan.nextExecutionTime.isDefined
  final val InactiveFilter: Table.Filter[PlanId, ExecutionPlan] =
    (id, plan) => !ActiveFilter(id, plan)

  final case class Props(proxy: ModelProxy[UserScope])
  final case class State(filter: Table.Filter[PlanId, ExecutionPlan])

  class Backend($ : BackendScope[Props, State]) {

    def mounted(props: Props): Callback = {
      val model = props.proxy()

      def loadJobs: Callback =
        Callback.when(model.jobSpecs.size == 0)(props.proxy.dispatch(LoadJobSpecs))

      def loadPlans: Callback =
        Callback.when(model.executionPlans.size == 0)(props.proxy.dispatch(LoadExecutionPlans))

      loadJobs >> loadPlans
    }

    def renderItem(
        model: UserScope)(planId: PlanId, plan: ExecutionPlan, column: String): ReactNode =
      column match {
        case "Job" =>
          val jobSpec = model.jobSpecs.get(plan.jobId)
          jobSpec.render(_.displayName)

        case "Current task"   => plan.currentTask.map(_.show).getOrElse(Cord.empty).toString()
        case "Trigger"        => plan.trigger.toString()
        case "Last Scheduled" => DateTimeDisplay(plan.lastScheduledTime)
        case "Last Execution" => DateTimeDisplay(plan.lastExecutionTime)
        case "Last Outcome"   => plan.lastOutcome.map(_.toString).getOrElse[String]("")
        case "Next Execution" => DateTimeDisplay(plan.nextExecutionTime)
      }

    def cancelPlan(props: Props)(planId: PlanId): Callback =
      props.proxy.dispatch(CancelExecutionPlan(planId))

    def rowActions(props: Props)(planId: PlanId, plan: ExecutionPlan) = {
      if (plan.nextExecutionTime.isDefined) {
        Seq(
          Table.RowAction[PlanId, ExecutionPlan](
            NonEmptyList(Icons.stop, "Cancel"),
            cancelPlan(props))
        )
      } else Seq.empty
    }

    def filterClicked(filterType: String): Callback = filterType match {
      case "All"      => $.modState(_.copy(filter = AllFilter))
      case "Active"   => $.modState(_.copy(filter = ActiveFilter))
      case "Inactive" => $.modState(_.copy(filter = InactiveFilter))
    }

    def render(props: Props, state: State) = {
      val model = props.proxy()
      NavBar(
        NavBar
          .Props(List("All", "Active", "Inactive"), "All", filterClicked, style = NavStyle.pills),
        Table(
          Columns,
          model.executionPlans.seq,
          renderItem(model),
          key = Some("executionPlans"),
          allowSelect = true,
          actions = Some(rowActions(props)(_, _)),
          filter = Some(state.filter))
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("ExecutionPlanList")
    .initialState(State(filter = AllFilter))
    .renderBackend[Backend]
    .componentDidMount($ => $.backend.mounted($.props))
    .build

  def apply(proxy: ModelProxy[UserScope]) =
    component.withKey("execution-plan-list")(Props(proxy))

}
