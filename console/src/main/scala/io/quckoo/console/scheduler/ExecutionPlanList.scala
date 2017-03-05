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

import io.quckoo.{ExecutionPlan, PlanId}
import io.quckoo.console.components._
import io.quckoo.console.core.ConsoleCircuit.Implicits.consoleClock
import io.quckoo.console.core.{LoadExecutionPlans, LoadJobSpecs, UserScope}
import io.quckoo.protocol.scheduler.CancelExecutionPlan

import japgolly.scalajs.react._

import org.threeten.bp.ZonedDateTime

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ExecutionPlanList {

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

  final case class Props(proxy: ModelProxy[UserScope])
  final case class State(filter: Table.Filter[PlanId, ExecutionPlan])

  class Backend($ : BackendScope[Props, State]) {

    def mounted(props: Props): Callback = {
      val model = props.proxy()

      def loadJobs: Callback =
        Callback.when(model.jobSpecs.size == 0)(props.proxy.dispatchCB(LoadJobSpecs))

      def loadPlans: Callback =
        Callback.when(model.executionPlans.size == 0)(props.proxy.dispatchCB(LoadExecutionPlans))

      loadJobs >> loadPlans
    }

    def renderItem(
        model: UserScope)(planId: PlanId, plan: ExecutionPlan, column: Symbol): ReactNode = {
      def displayDateTime(dateTime: ZonedDateTime): ReactNode =
        DateTimeDisplay(dateTime)

      column match {
        case 'Job =>
          val jobSpec = model.jobSpecs.get(plan.jobId)
          jobSpec.render(_.displayName)

        case 'Current   => plan.currentTask.map(_.show).getOrElse(Cord.empty).toString()
        case 'Trigger   => plan.trigger.toString()
        case 'Scheduled => plan.lastScheduledTime.map(displayDateTime).orNull
        case 'Execution => plan.lastExecutionTime.map(displayDateTime).orNull
        case 'Outcome   => plan.lastOutcome.map(_.toString).getOrElse[String]("")
        case 'Next      => plan.nextExecutionTime.map(displayDateTime).orNull
      }
    }

    def cancelPlan(props: Props)(planId: PlanId): Callback =
      props.proxy.dispatchCB(CancelExecutionPlan(planId))

    def rowActions(props: Props)(planId: PlanId, plan: ExecutionPlan) = {
      if (!plan.finished && plan.nextExecutionTime.isDefined) {
        Seq(
          Table.RowAction[PlanId, ExecutionPlan](
            NonEmptyList(Icons.stop, "Cancel"),
            cancelPlan(props))
        )
      } else Seq.empty
    }

    def filterClicked(filterType: Symbol): Callback = filterType match {
      case 'All      => $.modState(_.copy(filter = Table.NoFilter))
      case 'Active   => $.modState(_.copy(filter = ActiveFilter))
      case 'Inactive => $.modState(_.copy(filter = InactiveFilter))
    }

    def render(props: Props, state: State) = {
      val model = props.proxy()
      NavBar(
        NavBar
          .Props(List('All, 'Active, 'Inactive), 'All, filterClicked, style = NavStyle.pills),
        Table(
          Columns,
          model.executionPlans.seq,
          renderItem(model),
          key = Some("executionPlans"),
          actions = Some(rowActions(props)(_, _)),
          filter = Some(state.filter))
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("ExecutionPlanList")
    .initialState(State(filter = Table.NoFilter))
    .renderBackend[Backend]
    .componentDidMount($ => $.backend.mounted($.props))
    .build

  def apply(proxy: ModelProxy[UserScope]) =
    component.withKey("execution-plan-list")(Props(proxy))

}
