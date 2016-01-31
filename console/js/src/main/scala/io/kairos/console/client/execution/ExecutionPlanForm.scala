package io.kairos.console.client.execution

import io.kairos.console.client.core.ClientApi
import io.kairos.console.client.time.{AmountOfTime, AmountOfTimeField}
import io.kairos.id.JobId
import io.kairos.protocol._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.macros.Lenses

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ExecutionPlanForm {
  import MonocleReact._
  import SchedulerProtocol._

  type ScheduleHandler = ScheduleJob => Callback

  object TriggerOption extends Enumeration {
    type TriggerOption = Value
    val Immediate, After, Every, At = Value
  }

  @Lenses
  case class ScheduleDetails(jobId: JobId = null,
                             trigger: TriggerOption.TriggerOption = TriggerOption.Immediate,
                             timeout: AmountOfTime = AmountOfTime()) {
    def toScheduleJob: ScheduleJob = ???
  }

  @Lenses
  case class FormState(availableJobIds: IndexedSeq[(JobId, String)] = IndexedSeq.empty,
                       details: ScheduleDetails = ScheduleDetails())

  class ExecutionPlanFormBackend($: BackendScope[ScheduleHandler, FormState]) {

    def submitSchedule(event: ReactEventI): Callback = {
      event.preventDefaultCB >>
        $.state.flatMap(form => $.props.flatMap(handler => handler(form.details.toScheduleJob)))
    }

  }

  private[this] val component = ReactComponentB[ScheduleHandler]("ExecutionPlanForm").
    initialState(FormState()).
    backend(new ExecutionPlanFormBackend(_)).
    render { $ =>
      val jobId   = ExternalVar.state($.zoomL(FormState.details ^|-> ScheduleDetails.jobId))
      val trigger = ExternalVar.state($.zoomL(FormState.details ^|-> ScheduleDetails.trigger))
      val timeout = ExternalVar.state($.zoomL(FormState.details ^|-> ScheduleDetails.timeout))

      val updateJobId = (event: ReactEventI) => jobId.set($.state.availableJobIds(event.target.value.toInt)._1)
      val updateTrigger = (event: ReactEventI) => trigger.set(TriggerOption(event.target.value.toInt))

      <.form(^.name := "scheduleDetails", ^.onSubmit ==> $.backend.submitSchedule,
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "jobId", "Job"),
          <.select(^.id := "jobId", ^.`class` := "form-control", ^.required := true,
            ^.onChange ==> updateJobId,
            $.state.availableJobIds.zipWithIndex.map {
              case ((_, displayName), index) =>
                <.option(^.value := index, displayName)
            }
          )
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "trigger", "Trigger"),
          <.select(^.id := "trigger", ^.`class` := "form-control", ^.required := true,
            ^.value := trigger.value.id, ^.onChange ==> updateTrigger,
            TriggerOption.values.map { triggerOp =>
              <.option(^.value := triggerOp.id, triggerOp.toString())
            }
          )
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "timeout", "Timeout"),
          AmountOfTimeField("timeout", required = false, timeout)
        )
      )
    }.componentDidMount($ => Callback.future {
      ClientApi.enabledJobs map { specMap =>
        $.modState(_.copy(availableJobIds = specMap.map {
          case (jobId, spec) =>
            val desc = spec.description.map(d => s"| $d").getOrElse("")
            (jobId, s"${spec.displayName} $desc")
        } toIndexedSeq))
      }
    }).
    build

  def apply(scheduleHandler: ScheduleHandler) = component(scheduleHandler)

}
