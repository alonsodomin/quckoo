package io.kairos.console.client.scheduler

import diode.react.ModelProxy
import io.kairos.console.client.components.{Button, Icons}
import io.kairos.console.client.core.KairosModel
import io.kairos.console.model.Schedule
import io.kairos.protocol.SchedulerProtocol.ScheduleJob
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object SchedulerPage {

  object Style extends StyleSheet.Inline {
    import dsl._

    val content = style(addClassName("container"))
  }

  case class Props(proxy: ModelProxy[KairosModel])
  case class State(selectedSchedule: Option[Schedule] = None, showForm: Boolean = false)

  class ExecutionsBackend($: BackendScope[Props, State]) {

    def scheduleJob(scheduleJob: ScheduleJob): Callback = {
      def dispatchAction(props: Props): Callback =
        props.proxy.dispatch(scheduleJob)

      def updateState(): Callback =
        $.modState(_.copy(showForm = false))

      ($.props >>= dispatchAction) >> updateState()
    }

    def scheduleForm(schedule: Option[Schedule]) =
      $.modState(_.copy(selectedSchedule = schedule, showForm = true))

    def render(props: Props, state: State) = {
      <.div(Style.content,
        <.h2("Scheduler"),
        props.proxy().notification,
        Button(Button.Props(Some(scheduleForm(None))), Icons.plusSquare, "Create Schedule"),
        if (state.showForm) ScheduleForm(props.proxy, state.selectedSchedule, scheduleJob)
        else EmptyTag,
        props.proxy.wrap(_.schedules)(SchedulesList(_))
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("ExecutionsPage").
    initialState(State()).
    renderBackend[ExecutionsBackend].
    build

  def apply(proxy: ModelProxy[KairosModel]) = component(Props(proxy))

}
