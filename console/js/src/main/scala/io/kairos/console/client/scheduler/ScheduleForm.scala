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
import monocle.std.option._
import monocle.std.vector._
import monocle.function.all._

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
  case class Param(name: String, value: String)

  @Lenses
  case class ScheduleDetails(
      jobId: Option[JobId] = None,
      trigger: Option[Trigger] = None,
      params: Vector[Param] = Vector.empty,
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
    val params    = State.schedule ^|-> ScheduleDetails.params

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
              jobId.getOption(state).map(id => ^.value := id.toString()),
              ^.onChange ==> $._setStateL(jobId),
              <.option("Select a job"),
              props.proxy().jobSpecs.seq.map { case (id, spec) =>
                spec.renderReady { s =>
                  val desc = s.description.map(d => s"| $d").getOrElse("")
                  <.option(^.value := id.toString(),
                    s"${s.displayName} $desc"
                  )
                }
              }
            )
          )
        )
      }

      def triggerSelectorField: ReactNode = {

        def updateAfterTrigger(duration: FiniteDuration): Callback =
          $.setStateL(trigger)(Trigger.After(duration))

        def afterTriggerField: ReactNode =
          <.div(lnf.formGroup,
            <.label(^.`class` := "col-sm-2 control-label", "Delay"),
            <.div(^.`class` := "col-sm-12",
              FiniteDurationInput(
                "afterTrigger",
                trigger.getOption(state).
                  filter(_.isInstanceOf[Trigger.After]).
                  map(_.asInstanceOf[Trigger.After].delay).
                  getOrElse(0 seconds),
                updateAfterTrigger
              )
            )
          )

        def atTriggerField: ReactNode = {
          <.div(lnf.formGroup,
            <.div(^.`class` := "row",
              <.label(^.`class` := "col-sm-2 control-label", "DateTime")
            ),
            <.div(^.`class` := "row",
              <.div(^.`class` := "col-sm-5",
                <.input.date(lnf.formControl, ^.id := "atTrigger_date")
              ),
              <.div(^.`class` := "col-sm-5",
                <.input.time(lnf.formControl, ^.id := "atTrigger_time")
              )
            )
          )
        }

        def everyTriggerField: ReactNode = {
          def everyTriggerFreq = trigger.getOption(state).
            filter(_.isInstanceOf[Trigger.Every]).
            map(_.asInstanceOf[Trigger.Every].frequency).
            getOrElse(0 seconds)

          def everyTriggerDelay = trigger.getOption(state).
            filter(_.isInstanceOf[Trigger.Every]).
            flatMap(_.asInstanceOf[Trigger.Every].startingIn)

          def updateEveryTriggerFreq(duration: FiniteDuration): Callback =
            $.setStateL(trigger)(Trigger.Every(duration, everyTriggerDelay))

          def updateEveryTriggerDelay(duration: FiniteDuration): Callback =
            $.setStateL(trigger)(Trigger.Every(everyTriggerFreq, Some(duration)))

          Seq(
            <.div(lnf.formGroup,
              <.label(^.`class` := "col-sm-4 control-label", "Frequency"),
              <.div(^.`class` := "col-sm-12",
                FiniteDurationInput("everyTrigger_freq",
                  everyTriggerFreq, updateEveryTriggerFreq
                )
              )
            ),
            <.div(lnf.formGroup,
              <.label(^.`class` := "col-sm-4 control-label", "Initial delay"),
              <.div(^.`class` := "col-sm-12",
                FiniteDurationInput("everyTrigger_delay",
                  everyTriggerDelay.getOrElse(0 seconds),
                  updateEveryTriggerDelay
                )
              )
            )
          )
        }

        def fieldBody: ReactNode = {
          val triggerOpValue = triggerOp.get(state)
          val triggerField = (triggerOpValue match {
            case TriggerOption.After => Some(afterTriggerField)
            case TriggerOption.At    => Some(atTriggerField)
            case TriggerOption.Every => Some(everyTriggerField)
            case _                   => None
          }).map(html => <.div(^.`class` := "col-sm-offset-2", html))

          val triggerSelector: ReactNode = {
            <.div(lnf.formGroup,
              <.label(^.`class` := "col-sm-2 control-label", ^.`for` := "trigger", "Trigger"),
              <.div(^.`class` := "col-sm-10",
                <.select(lnf.formControl, ^.`for` := "trigger",
                  ^.value := triggerOpValue.id,
                  ^.onChange ==> updateTriggerOp,
                  TriggerOption.values.map { triggerOp =>
                    <.option(^.value := triggerOp.id, triggerOp.toString())
                  }
                )
              )
            )
          }

          <.div(triggerSelector, triggerField)
        }

        fieldBody
      }

      def timeoutField: ReactNode = <.div(
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

      def parameterListField: ReactNode = {

        def deleteParam(idx: Int): Callback = {
          def removeIdxFromVector(ps: Vector[Param]): Vector[Param] =
            ps.take(idx) ++ ps.drop(idx + 1)

          $.modState(st => params.modify(removeIdxFromVector)(st))
        }

        def parameterField(idx: Int, param: Param): ReactNode = {
          val indexFocus = params ^|-? index(idx)
          val paramName  = indexFocus ^|-> Param.name
          val paramValue = indexFocus ^|-> Param.value

          def updateParamName(evt: ReactEventI): Callback =
            $.setStateL(paramName)(evt.target.value)

          def updateParamValue(evt: ReactEventI): Callback =
            $.setStateL(paramValue)(evt.target.value)

          <.div(
            <.div(^.`class` := "col-sm-5",
              <.input.text(lnf.formControl, ^.value := param.name, ^.onChange ==> updateParamName)
            ),
            <.div(^.`class` := "col-sm-5",
              <.input.text(lnf.formControl, ^.value := param.value, ^.onChange ==> updateParamValue)
            ),
            <.div(^.`class` := "col-sm-2",
              Button(Button.Props(Some(deleteParam(idx)), style = ContextStyle.default), Icons.minus.noPadding)
            )
          )
        }

        def addParam(): Callback =
          $.modStateL(State.schedule)(st => st.copy(params = st.params :+ Param("", "")))

        <.div(
          <.div(lnf.formGroup,
            <.label(^.`class` := "col-sm-2 control-label", "Parameters"),
            <.div(^.`class` := "col-sm-10",
              Button(Button.Props(Some(addParam()), style = ContextStyle.default), Icons.plus.noPadding)
            )
          ),
          if (state.schedule.params.nonEmpty) <.div(
            <.div(^.`class` := "col-sm-offset-2",
              <.div(^.`class` := "col-sm-5",
                <.label(^.`class` := "control-label", "Name")
              ),
              <.div(^.`class` := "col-sm-5",
                <.label(^.`class` := "control-label", "Value")
              )
            ),
            state.schedule.params.zipWithIndex.map { case (p, idx) =>
              <.div(^.`class` := "col-sm-offset-2", parameterField(idx, p))
            }
          ) else EmptyTag
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
            Button(Button.Props(None, style = ContextStyle.default), "Preview"),
            Button(Button.Props(Some(submitForm() >> hide), style = ContextStyle.primary), "Ok")
          ),
          closed = formClosed(props, state)
        ),
        jobSelectorField,
        triggerSelectorField,
        timeoutField,
        parameterListField
      ))
    }

  }

  private[this] val component = ReactComponentB[Props]("ScheduleForm").
    initialState(State()).
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[KairosModel], schedule: Option[Schedule], handler: ScheduleHandler) =
    component(Props(proxy, schedule, handler))

}
