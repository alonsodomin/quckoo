package io.quckoo.console.scheduler

import diode.react.ModelProxy
import io.quckoo.ExecutionPlan
import io.quckoo.console.components.{Button, Icons}
import io.quckoo.console.core.ConsoleScope
import io.quckoo.protocol.scheduler.ScheduleJob
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object SchedulerPageView {

  object Style extends StyleSheet.Inline {
    import dsl._

    val content = style(addClassName("container"))
  }

  case class Props(proxy: ModelProxy[ConsoleScope])
  case class State(selectedSchedule: Option[ExecutionPlan] = None, showForm: Boolean = false)

  class ExecutionsBackend($: BackendScope[Props, State]) {

    def scheduleJob(scheduleJob: Option[ScheduleJob]): Callback = {
      def dispatchAction(props: Props): Callback =
        scheduleJob.map(props.proxy.dispatch).getOrElse(Callback.empty)

      def updateState(): Callback =
        $.modState(_.copy(showForm = false))

      updateState() >> ($.props >>= dispatchAction)
    }

    def scheduleForm(schedule: Option[ExecutionPlan]) =
      $.modState(_.copy(selectedSchedule = schedule, showForm = true))

    def render(props: Props, state: State) = {
      <.div(Style.content,
        <.h2("Scheduler"),
        props.proxy().notification,
        Button(Button.Props(Some(scheduleForm(None))), Icons.plusSquare, "Execution Plan"),
        if (state.showForm) {
          props.proxy.connect(_.jobSpecs)(ExecutionPlanForm(_, state.selectedSchedule, scheduleJob))
        } else EmptyTag,
        props.proxy.wrap(_.executionPlans)(ExecutionPlanList(_))
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("ExecutionsPage").
    initialState(State()).
    renderBackend[ExecutionsBackend].
    build

  def apply(proxy: ModelProxy[ConsoleScope]) = component(Props(proxy))

}
