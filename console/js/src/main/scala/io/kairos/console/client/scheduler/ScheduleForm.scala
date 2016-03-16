package io.kairos.console.client.scheduler

import diode.react.ModelProxy
import diode.react.ReactPot._
import io.kairos.Trigger
import io.kairos.console.client.components._
import io.kairos.console.client.core.{KairosModel, LoadJobSpecs}
import io.kairos.console.model.Schedule
import io.kairos.id.JobId
import io.kairos.protocol._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.macros.Lenses
import monocle.std.option.some

import scala.concurrent.duration._
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ScheduleForm {
  import MonocleReact._
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
      trigger: Option[Trigger] = None,
      timeout: Option[FiniteDuration] = None
  )

  case class Props(proxy: ModelProxy[KairosModel], schedule: Option[Schedule], handler: ScheduleHandler)

  @Lenses
  case class State(
      schedule: ScheduleDetails = ScheduleDetails(),
      triggerOp: TriggerOption.Value = TriggerOption.Immediate,
      enableTimeout: Boolean = false,
      cancelled: Boolean = true
  )

  class Backend($: BackendScope[Props, State]) {

    val jobId     = State.schedule ^|-> ScheduleDetails.jobId   ^<-? some
    val triggerOp = State.triggerOp
    val trigger   = State.schedule ^|-> ScheduleDetails.trigger ^<-? some
    val timeout   = State.schedule ^|-> ScheduleDetails.timeout ^<-? some

    def mounted(props: Props) =
      Callback.ifTrue(props.proxy().jobSpecs.size == 0, props.proxy.dispatch(LoadJobSpecs))

    def formClosed(props: Props, state: State) = {
      if (state.cancelled) Callback.empty
      else {
        def detailsToSchedule: ScheduleJob = ???
        props.handler(detailsToSchedule)
      }
    }

    def submitForm(): Callback =
      $.modState(_.copy(cancelled = false))

    def updateTriggerOp(evt: ReactEventI): Callback =
      $.setStateL(triggerOp)(TriggerOption(evt.target.value.toInt))

    def updateTimeoutFlag(evt: ReactEventI): Callback =
      $.modStateL(State.enableTimeout)(!_)

    def render(props: Props, state: State) = {

      def jobSelectorField = {
        <.div(lnf.formGroup,
          <.label(^.`class` := "col-sm-2 control-label", ^.`for` := "jobId", "Job"),
          <.div(^.`class` := "col-sm-10",
            <.select(lnf.formControl, ^.id := "jobId",
              <.option("Select a job"),
              props.proxy().jobSpecs.seq.map { case (id, spec) =>
                spec.renderReady { s =>
                  val desc = s.description.map(d => s"| $d").getOrElse("")
                  <.option(^.value := id.toString(),
                    jobId.getOption(state).map(x => ^.selected := (id == x)),
                    s"${s.displayName} $desc"
                  )
                }
              }
            )
          )
        )
      }

      def triggerSelectorField = {

        def updateAfterTrigger(duration: FiniteDuration): Callback =
          $.setStateL(trigger)(Trigger.After(duration))

        def afterTriggerField = {
          <.div(^.`class` := "col-sm-offset-2",
            FiniteDurationInput(
              "afterTrigger",
              trigger.getOption(state).
                filter(_.isInstanceOf[Trigger.After]).
                map(_.asInstanceOf[Trigger.After].delay).
                getOrElse(0 seconds),
              updateAfterTrigger
            )
          )
        }

        <.div(lnf.formGroup,
          <.label(^.`class` := "col-sm-2 control-label", ^.`for` := "trigger", "Trigger"),
          <.div(^.`class` := "col-sm-10",
            <.select(lnf.formControl, ^.`for` := "trigger",
              ^.value := triggerOp.get(state).id,
              ^.onChange ==> updateTriggerOp,
              TriggerOption.values.map { triggerOp =>
                <.option(^.value := triggerOp.id, triggerOp.toString())
              }
            )
          ),
          triggerOp.get(state) match {
            case TriggerOption.After => afterTriggerField
            case _ => EmptyTag
          }
        )
      }

      def timeoutField = {
        <.div(lnf.formGroup,
          <.label(^.`class` := "col-sm-2 control-label", "Timeout"),
          <.div(^.`class` := "col-sm-10",
            <.div(^.`class` := "checkbox",
              <.label(
                <.input.checkbox(
                  ^.id := "enableTimeout",
                  ^.value := State.enableTimeout.get(state),
                  ^.onChange ==> updateTimeoutFlag
                ),
                "Enabled"
              )
            )
          ),
          if (state.enableTimeout) {
            <.div(^.`class` := "col-sm-offset-2",
              FiniteDurationInput("timeout",
                timeout.getOption(state).getOrElse(0 seconds),
                $._setStateL(timeout)
              )
            )
          } else EmptyTag
        )
      }

      <.form(^.name := "scheduleDetails", ^.`class` := "form-horizontal", Modal(
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
        jobSelectorField,
        triggerSelectorField,
        timeoutField
      ))
    }

  }

  private[this] val component = ReactComponentB[Props]("ScheduleForm").
    initialState(State()).
    renderBackend[Backend].
    componentWillMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[KairosModel], schedule: Option[Schedule], handler: ScheduleHandler) =
    component(Props(proxy, schedule, handler))

}
