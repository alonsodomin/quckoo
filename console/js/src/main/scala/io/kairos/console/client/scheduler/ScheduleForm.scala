package io.kairos.console.client.scheduler

import diode.react.ModelProxy
import diode.react.ReactPot._
import io.kairos.console.client.components._
import io.kairos.console.client.core.KairosModel
import io.kairos.console.client.time.AmountOfTime
import io.kairos.console.model.Schedule
import io.kairos.id.JobId
import io.kairos.protocol._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.macros.Lenses
import monocle.std.option.some

import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ScheduleForm {
  import SchedulerProtocol._

  @inline
  private def lnf = lookAndFeel

  type ScheduleHandler = ScheduleJob => Callback

  object TriggerOption extends Enumeration {
    val Immediate, After, Every, At = Value
  }

  @Lenses
  case class ScheduleDetails(
      jobId: Option[JobId] = None,
      trigger: TriggerOption.Value = TriggerOption.Immediate,
      timeout: AmountOfTime = AmountOfTime()
  )

  case class Props(proxy: ModelProxy[KairosModel], schedule: Option[Schedule], handler: ScheduleHandler)

  @Lenses
  case class State(
      schedule: ScheduleDetails = ScheduleDetails(),
      enableTimeout: Boolean = false,
      cancelled: Boolean = true
  )

  class Backend($: BackendScope[Props, State]) {

    val jobId   = State.schedule ^|-> ScheduleDetails.jobId ^<-? some
    val trigger = State.schedule ^|-> ScheduleDetails.trigger
    val timeout = State.schedule ^|-> ScheduleDetails.timeout

    def formClosed(props: Props, state: State) = {
      if (state.cancelled) Callback.empty
      else {
        def detailsToSchedule: ScheduleJob = ???
        props.handler(detailsToSchedule)
      }
    }

    def submitForm(): Callback =
      $.modState(_.copy(cancelled = false))

    def render(props: Props, state: State) = {
      <.form(^.name := "scheduleDetails", Modal(
        Modal.Props(
          header = hide => <.span(
            <.button(^.tpe:= "button", lnf.close, ^.onClick --> hide, Icons.close),
            <.h4("Schedule job")
          ),
          footer = hide => <.span(
            Button(Button.Props(Some(hide), style = ContextStyle.default), "Cancel"),
            Button(Button.Props(Some(submitForm() >> hide), style = ContextStyle.primary), "Ok")
          ),
          closed = formClosed(props, state)
        ),
        <.div(lnf.formGroup,
          <.label(lnf.controlLabel, ^.`for` := "jobId", "Job"),
          <.select(lnf.formControl, ^.id := "jobId",
            props.proxy().jobSpecs.seq.map { case (id, spec) =>
              spec.renderReady { s =>
                val desc = s.description.map(d => s"| $d").getOrElse("")
                <.option(^.value := id.toString(), s"${s.displayName} $desc")
              }
            }
          )
        )
      ))
    }

  }

  /*private[this] val component = ReactComponentB[ScheduleHandler]("ExecutionPlanForm").
    initialState(FormState()).
    backend(new ExecutionPlanFormBackend(_)).
    render { $ =>
      val jobIdPrism : Optional[FormState, JobId] = FormState.details ^|-> ScheduleDetails.jobId ^<-? some
      val jobIdSetter = $.setStateL(jobIdPrism) _

      val trigger : ExternalVar[TriggerOption.Value] =
        ExternalVar.state($.zoomL(FormState.details ^|-> ScheduleDetails.trigger))
      val timeout : ExternalVar[AmountOfTime] =
        ExternalVar.state($.zoomL(FormState.details ^|-> ScheduleDetails.timeout))

      val updateJobId = (event: ReactEventI) => jobIdSetter($.state.availableJobIds(event.target.value)._1, Callback.empty)
      val updateTrigger = (event: ReactEventI) => trigger.set(TriggerOption(event.target.value.toInt))
      val updateEnableTimeout = (event: ReactEventI) => $.modState(form => form.copy(enableTimeout = !form.enableTimeout))

      <.form(^.name := "scheduleDetails", ^.`class` := "form-horizontal", ^.onSubmit ==> $.backend.submitSchedule,
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "jobId", ^.`class` := "col-sm-2 control-label", "Job"),
          <.div(^.`class` := "col-sm-10",
            <.select(^.id := "jobId", ^.`class` := "form-control", ^.required := true,
              ^.onChange ==> updateJobId, ^.value := $.state.details.jobId.map(_.toString),
              $.state.availableJobIds.map {
                case (id, (_, desc)) =>
                  <.option(^.value := id, desc)
              }
            )
          )
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "trigger", ^.`class` := "col-sm-2 control-label", "Trigger"),
          <.div(^.`class` := "col-sm-10",
            <.select(^.id := "trigger", ^.`class` := "form-control", ^.required := true,
              ^.value := trigger.value.id, ^.onChange ==> updateTrigger,
              TriggerOption.values.map { triggerOp =>
                <.option(^.value := triggerOp.id, triggerOp.toString())
              }
            )
          )
        ),
        <.div(^.`class` := "form-group",
          <.label(/*^.`for` := "timeout",*/ ^.`class` := "col-sm-2 control-label", "Timeout"),
          <.div(^.`class` := "col-sm-10",
            <.div(^.`class` := "checkbox",
              <.label(
                <.input.checkbox(^.id := "enableTimeout",
                  ^.value := $.state.enableTimeout,
                  ^.onChange ==> updateEnableTimeout
                ),
                "Enabled"
              )
            )
          ),
          <.div(^.`class` := "col-sm-offset-2",
            if ($.state.enableTimeout)
              AmountOfTimeInput("timeout", timeout)
            else EmptyTag
          )
        ),
        <.div(^.`class` := "col-sm-offset-2",
          <.button(^.`class` := "btn btn-default", "Submit")
        )
      )
    }.componentDidMount($ => Callback.future {
      ClientApi.enabledJobs map { specMap =>
        specMap.map {
          case (jobId, spec) =>
            val desc = spec.description.map(d => s"| $d").getOrElse("")
            dom.console.log(s"Collected job. id=$jobId, desc=$desc")
            (jobId.toString, (jobId, s"${spec.displayName} $desc"))
        }
      } map { idMap =>
        dom.console.log(s"idMap = $idMap")
        $.modState(_.copy(availableJobIds = idMap))
      }
    }).
    build*/

  private[this] val component = ReactComponentB[Props]("ScheduleForm").
    initialState(State()).
    renderBackend[Backend].
    build

  def apply(proxy: ModelProxy[KairosModel], schedule: Option[Schedule], handler: ScheduleHandler) =
    component(Props(proxy, schedule, handler))

}
