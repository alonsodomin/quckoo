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

import diode.AnyAction._
import diode.data.{PotMap, Ready}
import diode.react.ModelProxy

import io.quckoo.{ExecutionPlan, JobSpec, Trigger}
import io.quckoo.console.components._
import io.quckoo.console.core.LoadJobSpecs
import io.quckoo.console.registry.JobSelect
import io.quckoo.id.JobId
import io.quckoo.protocol.scheduler.ScheduleJob

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import monocle.macros.Lenses

import scala.concurrent.duration.FiniteDuration
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 08/04/2016.
  */
object ExecutionPlanForm {
  import MonocleReact._

  @inline
  private def lnf = lookAndFeel

  type ScheduleHandler = Option[ScheduleJob] => Callback

  @Lenses case class EditableExecutionPlan(jobId: Option[JobId] = None, trigger: Option[Trigger] = None) {

    def this(plan: Option[ExecutionPlan]) =
      this(plan.map(_.jobId), plan.map(_.trigger).orElse(Some(Trigger.Immediate)))

  }

  case class Props(proxy: ModelProxy[PotMap[JobId, JobSpec]], plan: Option[ExecutionPlan], handler: ScheduleHandler)
  @Lenses case class State(
      plan: EditableExecutionPlan,
      timeout: Option[FiniteDuration] = None,
      showPreview: Boolean = false,
      cancelled: Boolean = true
  )

  class Backend($: BackendScope[Props, State]) {

    val jobIdLens   = State.plan ^|-> EditableExecutionPlan.jobId
    val triggerLens = State.plan ^|-> EditableExecutionPlan.trigger
    val timeoutLens = State.timeout

    def mounted(props: Props) =
      Callback.when(props.proxy().size == 0)(props.proxy.dispatch(LoadJobSpecs))

    def togglePreview(): Callback =
      $.modState(st => st.copy(showPreview = !st.showPreview))

    def submitForm(): Callback =
      $.modState(_.copy(cancelled = false))

    def formClosed(props: Props, state: State): Callback = {
      if (state.cancelled) Callback.empty
      else {
        def command: Option[ScheduleJob] = for {
            jobId <- state.plan.jobId
            trigger <- state.plan.trigger
          } yield ScheduleJob(jobId, trigger, state.timeout)

        props.handler(command)
      }
    }

    def jobSpecs(props: Props): Map[JobId, JobSpec] =
      props.proxy().seq.flatMap {
        case (jobId, Ready(spec)) => Seq(jobId -> spec)
        case _                    => Seq()
      } toMap

    def onJobUpdated(value: Option[JobId]): Callback =
      $.setStateL(jobIdLens)(value)

    def onTriggerUpdate(value: Option[Trigger]): Callback =
      $.setStateL(triggerLens)(value)

    def onTimeoutUpdate(value: Option[FiniteDuration]): Callback =
      $.setStateL(timeoutLens)(value)

    // Not supported yet
    def onParamUpdate(name: String, value: String): Callback =
      Callback.empty

    def render(props: Props, state: State) = {
      <.form(^.name := "executionPlanForm", ^.`class` := "form-horizontal", Modal(
        Modal.Props(
          header = hide => <.span(
            <.button(^.tpe := "button", lnf.close, ^.onClick --> hide, Icons.close),
            <.h4("Execution plan")
          ),
          footer = hide => <.span(
            Button(Button.Props(Some(hide), style = ContextStyle.default), "Cancel"),
            Button(Button.Props(Some(togglePreview()), style = ContextStyle.default,
              disabled = jobIdLens.get(state).isEmpty || triggerLens.get(state).isEmpty),
              if (state.showPreview) "Back" else "Preview"
            ),
            Button(Button.Props(Some(submitForm() >> hide),
              disabled = jobIdLens.get(state).isEmpty || triggerLens.get(state).isEmpty,
              style = ContextStyle.primary), "Ok"
            )
          ),
          closed = formClosed(props, state)
        ),
        if (!state.showPreview) {
          <.div(
            JobSelect(jobSpecs(props), jobIdLens.get(state), onJobUpdated),
            TriggerSelect(triggerLens.get(state), onTriggerUpdate),
            ExecutionTimeoutInput(timeoutLens.get(state), onTimeoutUpdate),
            ExecutionParameterList(Map.empty, onParamUpdate)
          )
        } else {
          triggerLens.get(state).map(ExecutionPlanPreview(_)).get
        }
      ))
    }

  }

  val component = ReactComponentB[Props]("ExecutionPlanForm").
    initialState_P(props => State(new EditableExecutionPlan(props.plan))).
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[PotMap[JobId, JobSpec]], plan: Option[ExecutionPlan], handler: ScheduleHandler) =
    component(Props(proxy, plan, handler))

}
