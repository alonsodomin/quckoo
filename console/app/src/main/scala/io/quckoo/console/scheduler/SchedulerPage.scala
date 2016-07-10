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
import io.quckoo.console.components.{Button, Icons, TabPanel}
import io.quckoo.console.core.ConsoleScope
import io.quckoo.protocol.scheduler.ScheduleJob

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object SchedulerPage {

  object Style extends StyleSheet.Inline {
    import dsl._

    val content = style(addClassName("container"))
  }

  case class Props(proxy: ModelProxy[ConsoleScope])
  case class State(selectedSchedule: Option[ExecutionPlan] = None, showForm: Boolean = false)

  class ExecutionsBackend($: BackendScope[Props, State]) {

    def scheduleJob(scheduleJob: Option[ScheduleJob]): Callback = {
      def dispatchAction(props: Props): Callback =
        scheduleJob.map(props.proxy.dispatch[ScheduleJob]).getOrElse(Callback.empty)

      def updateState(): Callback =
        $.modState(_.copy(showForm = false))

      updateState() >> ($.props >>= dispatchAction)
    }

    def scheduleForm(schedule: Option[ExecutionPlan]) =
      $.modState(_.copy(selectedSchedule = schedule, showForm = true))

    def render(props: Props, state: State) = {
      val userScopeConnector = props.proxy.connect(_.userScope)
      val taskConnector = props.proxy.connect(_.userScope.tasks)

      <.div(Style.content,
        <.h2("Scheduler"),
        Button(Button.Props(Some(scheduleForm(None))), Icons.plusSquare, "Execution Plan"),
        if (state.showForm) {
          props.proxy.wrap(_.userScope.jobSpecs)(ExecutionPlanForm(_, state.selectedSchedule, scheduleJob))
        } else EmptyTag,
        TabPanel(
          "Execution Plans" -> userScopeConnector(ExecutionPlanList(_)),
          "Tasks"           -> taskConnector(TaskList(_))
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("ExecutionsPage").
    initialState(State()).
    renderBackend[ExecutionsBackend].
    build

  def apply(proxy: ModelProxy[ConsoleScope]) = component(Props(proxy))

}
