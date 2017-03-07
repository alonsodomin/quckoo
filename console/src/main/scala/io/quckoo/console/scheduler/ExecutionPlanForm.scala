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

import io.quckoo.{ExecutionPlan, JobId, JobSpec, Trigger}
import io.quckoo.console.components._
import io.quckoo.console.core.ConsoleCircuit.Implicits.consoleClock
import io.quckoo.console.core.LoadJobSpecs
import io.quckoo.console.registry.JobSelect
import io.quckoo.protocol.scheduler.ScheduleJob

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
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

  type Handler = Option[ScheduleJob] => Callback

  @Lenses final case class EditableExecutionPlan(
    jobId: Option[JobId] = None,
    trigger: Option[Trigger] = None
  ) {

    def this(plan: Option[ExecutionPlan]) =
      this(plan.map(_.jobId), plan.map(_.trigger).orElse(Some(Trigger.Immediate)))

    def valid: Boolean =
      jobId.nonEmpty && trigger.nonEmpty

  }

  final case class Props(
      proxy: ModelProxy[PotMap[JobId, JobSpec]],
      handler: Handler
  )
  @Lenses final case class State(
      plan: EditableExecutionPlan,
      timeout: Option[FiniteDuration] = None,
      showPreview: Boolean = false,
      visible: Boolean = false,
      readOnly: Boolean = false,
      cancelled: Boolean = true
  )

  class Backend($: BackendScope[Props, State]) {

    val jobIdLens   = State.plan ^|-> EditableExecutionPlan.jobId
    val triggerLens = State.plan ^|-> EditableExecutionPlan.trigger
    val timeoutLens = State.timeout

    private[ExecutionPlanForm] def initialize(props: Props) =
      Callback.when(props.proxy().size == 0)(props.proxy.dispatchCB(LoadJobSpecs))

    // Event handlers

    def onModalClosed(props: Props, state: State): Callback = {
      def command: Option[ScheduleJob] = if (!state.cancelled) {
        for {
          jobId <- state.plan.jobId
          trigger <- state.plan.trigger
        } yield ScheduleJob(jobId, trigger, state.timeout)
      } else None

      $.modState(_.copy(visible = false)) >> props.handler(command)
    }

    // Not supported yet
    def onParamUpdate(name: String, value: String): Callback =
      Callback.empty

    // Actions

    def togglePreview(): Callback =
      $.modState(st => st.copy(showPreview = !st.showPreview))

    def submitForm(): Callback =
      $.modState(_.copy(visible = false, cancelled = false))

    def editPlan(plan: Option[ExecutionPlan]): Callback =
      $.modState(_.copy(plan = new EditableExecutionPlan(plan), visible = true, readOnly = plan.isDefined))

    // Rendering

    def jobSpecs(props: Props): Map[JobId, JobSpec] = {
      props.proxy().seq.flatMap {
        case (jobId, Ready(spec)) => Seq(jobId -> spec)
        case _                    => Seq()
      } toMap
    }

    def render(props: Props, state: State) = {
      def formHeader(hide: Callback) = {
        <.span(
          <.button(^.tpe := "button", lnf.close, ^.onClick --> hide, Icons.close),
          <.h4("Execution plan")
        )
      }

      def formFooter(hide: Callback) = {
        <.span(
          Button(Button.Props(Some(hide), style = ContextStyle.default), "Cancel"),
          Button(Button.Props(Some(togglePreview()), style = ContextStyle.default,
            disabled = jobIdLens.get(state).isEmpty || triggerLens.get(state).isEmpty),
            if (state.showPreview) "Back" else "Preview"
          ),
          Button(Button.Props(Some(submitForm() >> hide),
            disabled = state.readOnly || !state.plan.valid,
            style = ContextStyle.primary), "Save"
          )
        )
      }

      <.form(^.name := "executionPlanForm", ^.`class` := "form-horizontal",
        if (state.visible) {
          Modal(
            Modal.Props(
              header = formHeader,
              footer = formFooter,
              onClosed = onModalClosed(props, state)
            ),
            if (!state.showPreview) {
              <.div(
                JobSelect(jobSpecs(props), jobIdLens.get(state), $.setStateL(jobIdLens)(_)),
                TriggerSelect(triggerLens.get(state), $.setStateL(triggerLens)(_)),
                ExecutionTimeoutInput(timeoutLens.get(state), $.setStateL(timeoutLens)(_))
                //ExecutionParameterList(Map.empty, onParamUpdate)
              )
            } else {
              triggerLens.get(state).map(ExecutionPlanPreview(_)).get
            }
          )
        } else EmptyTag
      )
    }

  }

  val component = ReactComponentB[Props]("ExecutionPlanForm")
    .initialState(State(EditableExecutionPlan(None)))
    .renderBackend[Backend]
    .componentDidMount($ => $.backend.initialize($.props))
    .build

  def apply(proxy: ModelProxy[PotMap[JobId, JobSpec]], handler: Handler, refName: String) =
    component.withRef(refName)(Props(proxy, handler))

}
