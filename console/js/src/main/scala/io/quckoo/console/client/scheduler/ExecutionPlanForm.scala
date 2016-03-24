package io.quckoo.console.client.scheduler

import diode.data.{Empty, PotMap}
import diode.data.PotState.PotEmpty
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.quckoo.{ExecutionPlan, JobSpec, Trigger}
import io.quckoo.console.client.components._
import io.quckoo.console.client.core.{ConsoleModel, LoadJobSpecs}
import io.quckoo.id.JobId
import io.quckoo.protocol._
import io.quckoo.time._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.function.all._
import monocle.macros.Lenses
import monocle.std.vector._
import org.widok.moment.{Moment, Date => MDate}

import scala.concurrent.duration._
import scala.scalajs.js.Date
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ExecutionPlanForm {
  import MonocleReact._
  import SchedulerProtocol._

  @inline
  private def lnf = lookAndFeel

  // Component properties

  type ScheduleHandler = ScheduleJob => Callback

  case class Props(proxy: ModelProxy[ConsoleModel], plan: Option[ExecutionPlan], handler: ScheduleHandler)

  // Component model / state

  object TriggerOption extends Enumeration {
    val Immediate, After, Every, At = Value
  }

  @Lenses
  case class Param(name: String, value: String)

  @Lenses
  case class PlanDetails(
      jobId: Option[JobId] = None,
      trigger: Trigger = Trigger.Immediate,
      params: Vector[Param] = Vector.empty,
      timeout: Option[FiniteDuration] = None
  )

  @Lenses
  case class State(
      plan: PlanDetails = PlanDetails(),
      triggerOp: TriggerOption.Value = TriggerOption.Immediate,
      enableTimeout: Boolean = false,
      cancelled: Boolean = true
  )

  class Backend($: BackendScope[Props, State]) {

    val jobId     = State.plan ^|-> PlanDetails.jobId
    val triggerOp = State.triggerOp
    val trigger   = State.plan ^|-> PlanDetails.trigger
    val timeout   = State.plan ^|-> PlanDetails.timeout
    val params    = State.plan ^|-> PlanDetails.params

    def mounted(props: Props) =
      Callback.ifTrue(props.proxy().jobSpecs.size == 0, props.proxy.dispatch(LoadJobSpecs))

    def formClosed(props: Props, state: State) = {
      if (state.cancelled) Callback.empty
      else {
        def detailsToSchedule: ScheduleJob =
          ScheduleJob(state.plan.jobId.get,
            trigger = state.plan.trigger,
            timeout = state.plan.timeout)

        props.handler(detailsToSchedule)
      }
    }

    def submitForm(): Callback =
      $.modState(_.copy(cancelled = false))

    def render(props: Props, state: State) = {

      def jobSelectorField = {

        def updateJobId(evt: ReactEventI): Callback = {
          if (evt.target.value isEmpty)
            $.setStateL(jobId)(None)
          else
            $.setStateL(jobId)(Some(JobId(evt.target.value)))
        }

        def enabledJobs = {
          val jobs = props.proxy().jobSpecs
          jobs.seq.map { case (id, pot) =>
            if (pot.exists(_.disabled)) (id, Empty)
            else (id, pot)
          }
        }

        <.div(lnf.formGroup,
          <.label(^.`class` := "col-sm-2 control-label", ^.`for` := "jobId", "Job"),
          <.div(^.`class` := "col-sm-10",
            <.select(lnf.formControl, ^.id := "jobId",
              jobId.get(state).map(id => ^.value := id.toString()),
              ^.onChange ==> updateJobId,
              <.option("Select a job"),
              enabledJobs.map { case (id, spec) =>
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

        def updateTriggerOp(evt: ReactEventI): Callback = {
          val selection = TriggerOption(evt.target.value.toInt)
          $.setStateL(triggerOp)(selection).ret(selection) flatMap {
            case TriggerOption.Immediate => $.setStateL(trigger)(Trigger.Immediate)
            case _ => Callback.empty
          }
        }

        def afterTriggerField: ReactNode = {

          def updateAfterTrigger(duration: FiniteDuration): Callback =
            $.setStateL(trigger)(Trigger.After(duration))

          <.div(lnf.formGroup,
            <.label(^.`class` := "col-sm-2 control-label", "Delay"),
            <.div(^.`class` := "col-sm-12",
              FiniteDurationInput(
                "afterTrigger",
                Option(trigger.get(state)).
                  filter(_.isInstanceOf[Trigger.After]).
                  map(_.asInstanceOf[Trigger.After].delay).
                  getOrElse(0 seconds),
                updateAfterTrigger
              )
            )
          )
        }

        def atTriggerField: ReactNode = {
          lazy val atTrigger = Option(trigger.get(state)).
            filter(_.isInstanceOf[Trigger.At]).
            map(_.asInstanceOf[Trigger.At])

          lazy val underlyingDate = atTrigger.
            map(t => t.when).
            map(dt => dt.underlying.asInstanceOf[MDate])

          lazy val datePart = underlyingDate.
            orElse(Some(Moment())).
            map(d => d.format("YYYY-MM-DD")).get
          lazy val timePart = underlyingDate.
            map(d => d.format("HH:mm")).getOrElse("00:00")

          def dateTimeOf(date: MDate, time: MDate): DateTime = {
            val jsDate = date.toDate()
            val jsTime = time.toDate()
            val jsDateTime = new Date(
              jsDate.getFullYear(), jsDate.getMonth(), jsDate.getDate(),
              jsTime.getHours(), jsTime.getMinutes()
            )
            new MomentJSDateTime(Moment(jsDateTime))
          }

          def updateDate(evt: ReactEventI): Callback = {
            val date = Moment(evt.target.value)
            val time = Moment(timePart, "HH:mm")

            $.setStateL(trigger)(Trigger.At(dateTimeOf(date, time)))
          }

          def updateTime(evt: ReactEventI): Callback = {
            val date = Moment(datePart, "YYYY-MM-DD")
            val time = Moment(evt.target.value, "HH:mm")

            $.setStateL(trigger)(Trigger.At(dateTimeOf(date, time)))
          }

          <.div(
            <.div(lnf.formGroup,
              <.label(^.`class` := "col-sm-2 control-label", "Date"),
              <.div(^.`class` := "col-sm-4"),
              <.label(^.`class` := "col-sm-2 control-label", "Time"),
              <.div(^.`class` := "col-sm-4"),
              <.div(^.`class` := "col-sm-6",
                <.input.date(lnf.formControl, ^.id := "atTrigger_date",
                  ^.value := datePart,
                  ^.onChange ==> updateDate
                )
              ),
              <.div(^.`class` := "col-sm-6",
                <.input.time(lnf.formControl, ^.id := "atTrigger_time",
                  ^.value := timePart,
                  ^.onChange ==> updateTime
                )
              )
            )
          )
        }

        def everyTriggerField: ReactNode = {
          lazy val everyTriggerFreq = Option(trigger.get(state)).
            filter(_.isInstanceOf[Trigger.Every]).
            map(_.asInstanceOf[Trigger.Every].frequency).
            getOrElse(0 seconds)

          lazy val everyTriggerDelay = Option(trigger.get(state)).
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

      def timeoutField: ReactNode = {
        def updateTimeoutFlag(evt: ReactEventI): Callback =
          $.modStateL(State.enableTimeout)(!_)

        <.div(
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
                timeout.get(state).getOrElse(0 seconds),
                value => $.modState(st => timeout.set(Some(value))(st))
              )
            )
          } else EmptyTag
        )
      }

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
          $.modStateL(State.plan)(st => st.copy(params = st.params :+ Param("", "")))

        <.div(
          <.div(lnf.formGroup,
            <.label(^.`class` := "col-sm-2 control-label", "Parameters"),
            <.div(^.`class` := "col-sm-10",
              Button(Button.Props(Some(addParam()), style = ContextStyle.default), Icons.plus.noPadding)
            )
          ),
          if (state.plan.params.nonEmpty) <.div(
            <.div(^.`class` := "col-sm-offset-2",
              <.div(^.`class` := "col-sm-5",
                <.label(^.`class` := "control-label", "Name")
              ),
              <.div(^.`class` := "col-sm-5",
                <.label(^.`class` := "control-label", "Value")
              )
            ),
            state.plan.params.zipWithIndex.map { case (p, idx) =>
              <.div(^.`class` := "col-sm-offset-2", parameterField(idx, p))
            }
          ) else EmptyTag
        )
      }

      <.form(^.name := "executionPlanForm", ^.`class` := "form-horizontal", Modal(
        Modal.Props(
          header = hide => <.span(
            <.button(^.tpe:= "button", lnf.close, ^.onClick --> hide, Icons.close),
            <.h4("Execution plan")
          ),
          footer = hide => <.span(
            Button(Button.Props(Some(hide), style = ContextStyle.default), "Cancel"),
            Button(Button.Props(None, style = ContextStyle.default), "Preview"),
            Button(Button.Props(Some(submitForm() >> hide),
              disabled = jobId.get(state).isEmpty,
              style = ContextStyle.primary
            ), "Ok")
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

  private[this] val component = ReactComponentB[Props]("ExecutionPlanForm").
    initialState(State()).
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[ConsoleModel], schedule: Option[ExecutionPlan], handler: ScheduleHandler) =
    component(Props(proxy, schedule, handler))

}
