package io.kairos.console.client.execution

import io.kairos.console.client.components.AmountOfTimeInput
import io.kairos.console.client.core.ClientApi
import io.kairos.console.client.time.AmountOfTime
import io.kairos.id.JobId
import io.kairos.protocol._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.Optional
import monocle.macros.Lenses
import monocle.std.option.some
import org.scalajs.dom

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ExecutionPlanForm {
  import MonocleReact._
  import SchedulerProtocol._

  type ScheduleHandler = ScheduleJob => Callback

  object TriggerOption extends Enumeration {
    val Immediate, After, Every, At = Value
  }

  @Lenses
  case class ScheduleDetails(jobId: Option[JobId] = None,
                             trigger: TriggerOption.Value = TriggerOption.Immediate,
                             timeout: AmountOfTime = AmountOfTime())

  @Lenses
  case class FormState(availableJobIds: Map[String, (JobId, String)] = Map.empty,
                       enableTimeout: Boolean = false,
                       details: ScheduleDetails = ScheduleDetails()) {

    def toScheduleJob: CallbackTo[ScheduleJob] = ???

  }

  class ExecutionPlanFormBackend($: BackendScope[ScheduleHandler, FormState]) {

    val jobId   = FormState.details ^|-> ScheduleDetails.jobId ^<-? some
    val trigger = FormState.details ^|-> ScheduleDetails.trigger
    val timeout = FormState.details ^|-> ScheduleDetails.timeout

    def submitSchedule(event: ReactEventI): Callback = {
      event.preventDefaultCB >>
        $.state.flatMap(form => $.props.flatMap(handler => handler(form.toScheduleJob.runNow())))
    }

  }

  private[this] val component = ReactComponentB[ScheduleHandler]("ExecutionPlanForm").
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
    build

  def apply(scheduleHandler: ScheduleHandler) = component(scheduleHandler)

}
