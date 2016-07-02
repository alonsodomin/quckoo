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

import diode.data.{Pot, PotMap}
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.quckoo.ExecutionPlan
import io.quckoo.console.components.Notification
import io.quckoo.console.core.{LoadExecutionPlans, LoadJobSpecs, UserScope}
import io.quckoo.fault.ExceptionThrown
import io.quckoo.id.PlanId
import io.quckoo.time.MomentJSTimeSource.Implicits.default
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ExecutionPlanList {

  private[this] type RowAction = PlanId => Callback

  private[this] final case class RowProps(
      planId: PlanId,
      plan: Pot[ExecutionPlan],
      scope: UserScope,
      selected: Boolean,
      toggleSelected: RowAction
  )

  private[this] val PlanRow = ReactComponentB[RowProps]("ExecutionPlanRow").
    stateless.
    render_P { case RowProps(planId, plan, scope, selected, toggleSelected) =>
      <.tr(selected ?= (^.`class` := "info"),
        plan.renderFailed { ex =>
          <.td(^.colSpan := 6, Notification.danger(ExceptionThrown(ex)))
        },
        plan.renderPending { _ =>
          <.td(^.colSpan := 6, "Loading ...")
        },
        plan.render { item =>
          val jobSpec = scope.jobSpecs.get(item.jobId)
          List(
            <.td(<.input.checkbox(
              ^.id := s"selectPlan_$planId",
              ^.value := selected,
              ^.onChange --> toggleSelected(planId)
            )),
            <.td(jobSpec.render(_.displayName)),
            <.td(item.currentTaskId.map(_.toString())),
            <.td(item.trigger.toString()),
            <.td(item.lastScheduledTime.map(_.toString())),
            <.td(item.lastExecutionTime.map(_.toString())),
            <.td(item.lastOutcome.toString()),
            <.td(item.nextExecutionTime(default).toString())
          )
        }
      )
    } build

  final case class Props(proxy: ModelProxy[UserScope])
  final case class State(selected: Set[PlanId], allSelected: Boolean = false)

  class Backend($: BackendScope[Props, State]) {

    def mounted(props: Props): Callback = {
      val model = props.proxy()

      def loadJobs: Callback =
        Callback.when(model.jobSpecs.size == 0)(props.proxy.dispatch(LoadJobSpecs))

      def loadPlans: Callback =
        Callback.when(model.executionPlans.size == 0)(props.proxy.dispatch(LoadExecutionPlans))

      loadJobs >> loadPlans
    }

    def toggleSelected(props: Props)(planId: PlanId): Callback = {
      $.modState { state =>
        val newSet = {
          if (state.selected.contains(planId))
            state.selected - planId
          else state.selected + planId
        }
        state.copy(selected = newSet, allSelected = newSet.size == props.proxy().executionPlans.size)
      }
    }

    def toggleSelectAll(props: Props): Callback = {
      $.modState { state =>
        if (state.allSelected) state.copy(selected = Set.empty, allSelected = false)
        else state.copy(selected = props.proxy().executionPlans.keySet, allSelected = true)
      }
    }

    def render(props: Props, state: State) = {
      val model = props.proxy()
      <.table(^.`class` := "table table-striped",
        <.thead(
          <.tr(
            <.th(<.input.checkbox(
              ^.id := "selectAllPlans",
              ^.value := false,
              ^.onChange --> toggleSelectAll(props)
            )),
            <.th("Job"),
            <.th("Current task"),
            <.th("Trigger"),
            <.th("Last Scheduled"),
            <.th("Last Execution"),
            <.th("Last Outcome"),
            <.th("Next Execution")
          )
        ),
        <.tbody(
          model.executionPlans.seq.map { case (planId, plan) =>
            PlanRow.withKey(planId.toString)(
              RowProps(planId, plan, model,
                state.selected.contains(planId),
                toggleSelected(props)
              )
            )
          }
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("ExecutionPlanList").
    initialState(State(Set.empty)).
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[UserScope]) =
    component.withKey("execution-plan-list")(Props(proxy))

}
