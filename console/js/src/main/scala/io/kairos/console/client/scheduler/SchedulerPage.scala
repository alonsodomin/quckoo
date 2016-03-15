package io.kairos.console.client.scheduler

import diode.react.ModelProxy
import io.kairos.console.client.core.KairosModel
import io.kairos.console.client.layout._
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
  case class State()

  class ExecutionsBackend($: BackendScope[Props, State]) {

    def handleSchedule(scheduleJob: ScheduleJob): Callback = {
      //$.modState(st => st.copy(notifications = Seq()))
      Callback.empty
    }

    def render(props: Props, state: State) = {
      <.div(Style.content,
        <.h2("Executions"),
        Panel("Schedule a job",
          //NotificationDisplay(state.notifications),
          ExecutionPlanForm(handleSchedule)
        ),
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
