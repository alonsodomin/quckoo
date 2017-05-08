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

import io.quckoo.ExecutionPlan
import io.quckoo.console.components._
import io.quckoo.console.core.ConsoleScope
import io.quckoo.console.layout.CssSettings._
import io.quckoo.protocol.scheduler.ScheduleJob

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 17/10/2015.
  */
object SchedulerPage {

  object Style extends StyleSheet.Inline {
    import dsl._

    val content = style(addClassName("container"))
  }

  final case class Props(proxy: ModelProxy[ConsoleScope])

  class Backend($ : BackendScope[Props, Unit]) {

    private val executionPlanFormRef = ScalaComponent.mutableRefTo(ExecutionPlanForm.component)

    def scheduleJob(scheduleJob: Option[ScheduleJob]): Callback = {
      def dispatchAction(props: Props): Callback =
        scheduleJob.map(props.proxy.dispatchCB[ScheduleJob]).getOrElse(Callback.empty)

      $.props >>= dispatchAction
    }

    def editPlan(plan: Option[ExecutionPlan]): Callback = {
      CallbackTo(executionPlanFormRef).flatMap(_.value.backend.editPlan(plan))
    }

    def render(props: Props) = {
      val userScopeConnector = props.proxy.connect(_.userScope)
      val executionConnector = props.proxy.connect(_.userScope.executions)

      <.div(
        Style.content,
        <.h2("Scheduler"),
        props.proxy.wrap(_.userScope.jobSpecs) { jobs =>
          executionPlanFormRef.component(ExecutionPlanForm.Props(jobs, scheduleJob))
        },
        TabPanel(
          'Plans      -> userScopeConnector(ExecutionPlanList(_, editPlan(None), plan => editPlan(Some(plan)))),
          'Executions -> executionConnector(TaskExecutionList(_))
        )
      )
    }

  }

  private[this] val component = ScalaComponent.builder[Props]("ExecutionsPage")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[ConsoleScope]) = component(Props(proxy))

}
