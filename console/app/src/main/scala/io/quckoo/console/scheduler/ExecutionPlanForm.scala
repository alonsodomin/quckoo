package io.quckoo.console.scheduler

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

  type ScheduleHandler = ScheduleJob => Callback

  @Lenses case class EditableExecutionPlan(jobId: Option[JobId] = None, trigger: Option[Trigger] = None) {

    def this(plan: Option[ExecutionPlan]) =
      this(plan.map(_.jobId), plan.map(_.trigger).orElse(Some(Trigger.Immediate)))

  }

  case class Props(proxy: ModelProxy[PotMap[JobId, JobSpec]], plan: Option[ExecutionPlan], handler: ScheduleHandler)
  @Lenses case class State(plan: EditableExecutionPlan, timeout: Option[FiniteDuration] = None, cancelled: Boolean = true)

  class Backend($: BackendScope[Props, State]) {

    val jobIdLens   = State.plan ^|-> EditableExecutionPlan.jobId
    val triggerLens = State.plan ^|-> EditableExecutionPlan.trigger
    val timeoutLens = State.timeout

    def mounted(props: Props) =
      Callback.ifTrue(props.proxy().size == 0, props.proxy.dispatch(LoadJobSpecs))

    def submitForm(): Callback =
      $.modState(_.copy(cancelled = false))

    def formClosed(props: Props, state: State): Callback = {
      if (state.cancelled) Callback.empty
      else {
        def buildCommand: Option[ScheduleJob] = for {
            jobId <- state.plan.jobId
            trigger <- state.plan.trigger
          } yield ScheduleJob(jobId, trigger, state.timeout)

        buildCommand.map(cmd => props.handler(cmd)).getOrElse(Callback.empty)
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
            Button(Button.Props(None, style = ContextStyle.default), "Preview"),
            Button(Button.Props(Some(submitForm() >> hide),
              disabled = jobIdLens.get(state).isEmpty || triggerLens.get(state).isEmpty,
              style = ContextStyle.primary), "Ok"
            )
          ),
          closed = formClosed(props, state)
        ),
        JobSelect(jobSpecs(props), jobIdLens.get(state), onJobUpdated),
        TriggerSelect(triggerLens.get(state), onTriggerUpdate),
        ExecutionTimeoutInput(timeoutLens.get(state), onTimeoutUpdate),
        ExecutionParameterList(Map.empty, onParamUpdate)
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
