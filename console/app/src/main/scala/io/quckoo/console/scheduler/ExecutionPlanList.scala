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
import io.quckoo.console.core.LoadExecutionPlans
import io.quckoo.fault.ExceptionThrown
import io.quckoo.id.PlanId
import io.quckoo.time.MomentJSTimeSource.Implicits.default

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ExecutionPlanList {

  case class RowProps(planId: PlanId, plan: Pot[ExecutionPlan])

  val PlanRow = ReactComponentB[RowProps]("ExecutionPlanRow").
    stateless.
    render_P { case RowProps(planId, plan) =>
      <.tr(
        plan.renderFailed { ex =>
          <.td(^.colSpan := 6, Notification.danger(ExceptionThrown(ex)))
        },
        plan.renderPending { _ =>
          <.td(^.colSpan := 6, "Loading ...")
        },
        plan.render { item => List(
          <.td(item.jobId.toString()),
          <.td(item.planId.toString()),
          <.td(item.trigger.toString()),
          <.td(item.lastScheduledTime.map(_.toString())),
          <.td(item.lastExecutionTime.map(_.toString())),
          <.td(item.lastOutcome.toString()),
          <.td(item.nextExecutionTime(default).toString())
        )}
      )
    } build

  case class Props(proxy: ModelProxy[PotMap[PlanId, ExecutionPlan]])

  class Backend($: BackendScope[Props, Unit]) {

    def mounted(props: Props): Callback = {
      def perform: Callback =
        props.proxy.dispatch(LoadExecutionPlans)

      Callback.when(props.proxy().size == 0)(perform)
    }

    def render(p: Props) = {
      val model = p.proxy()
      <.table(^.`class` := "table table-striped",
        <.thead(
          <.tr(
            <.th("Job ID"),
            <.th("Plan ID"),
            <.th("Trigger"),
            <.th("Last Scheduled"),
            <.th("Last Execution"),
            <.th("Last Outcome"),
            <.th("Next Execution")
          )
        ),
        <.tbody(
          model.seq.map { case (planId, plan) =>
            PlanRow.withKey(planId.toString)(RowProps(planId, plan))
          }
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("ExecutionPlanList").
    stateless.
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[PotMap[PlanId, ExecutionPlan]]) = component(Props(proxy))

}
