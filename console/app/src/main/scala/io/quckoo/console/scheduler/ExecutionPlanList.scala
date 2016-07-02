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
import io.quckoo.time.MomentJSTimeSource.Implicits.default

import japgolly.scalajs.react._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ExecutionPlanList {

  final val Columns = List(
    "Job", "Current task", "Trigger", "Last Scheduled", "Last Execution",
    "Last Outcome", "Next Execution"
  )

  final case class Props(proxy: ModelProxy[UserScope])

  class Backend($: BackendScope[Props, Unit]) {

    def mounted(props: Props): Callback = {
      val model = props.proxy()

      def loadJobs: Callback =
        Callback.when(model.jobSpecs.size == 0)(props.proxy.dispatch(LoadJobSpecs))

      def loadPlans: Callback =
        Callback.when(model.executionPlans.size == 0)(props.proxy.dispatch(LoadExecutionPlans))

      loadJobs >> loadPlans
    }

    def renderItem(model: UserScope)(planId: PlanId, plan: ExecutionPlan, column: String): ReactNode = column match {
      case "Job" =>
        val jobSpec = model.jobSpecs.get(plan.jobId)
        jobSpec.render(_.displayName)

      case "Current task" => plan.currentTaskId.map(_.toString()).getOrElse[String]("")
      case "Trigger" => plan.trigger.toString()
      case "Last Scheduled" => plan.lastScheduledTime.map(_.toString()).getOrElse[String]("")
      case "Last Execution" => plan.lastExecutionTime.map(_.toString()).getOrElse[String]("")
      case "Last Outcome"   => plan.lastOutcome.toString
      case "Next Execution" =>
        val nextExecution = plan.nextExecutionTime
        nextExecution.map(_.toString).getOrElse[String]("")
    }

    def render(props: Props) = {
      val model = props.proxy()
      Table(Columns, model.executionPlans.seq, renderItem(model))
    }

  }

  private[this] val component = ReactComponentB[Props]("ExecutionPlanList").
    stateless.
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[UserScope]) =
    component.withKey("execution-plan-list")(Props(proxy))

}
