package io.kairos.console.client.execution

import io.kairos.console.client.layout._
import io.kairos.protocol.SchedulerProtocol.ScheduleJob
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object ExecutionsPage {

  object Style extends StyleSheet.Inline {
    val content = style()
  }

  case class State(notifications: Seq[Notification] = Seq(),
                   plans: Seq[String] = Seq())

  class ExecutionsBackend($: BackendScope[Unit, State]) {

    def handleSchedule(scheduleJob: ScheduleJob): Callback = {
      $.modState(st => st.copy(notifications = Seq()))
    }

    def render(state: State) = {
      <.div(Style.content,
        <.h2("Executions"),
        Panel("Schedule a job",
          NotificationDisplay(state.notifications),
          ExecutionPlanForm(handleSchedule)
        ),
        ExecutionPlanList(state.plans)
      )
    }

  }

  private[this] val component = ReactComponentB[Unit]("ExecutionsPage").
    initialState(State()).
    renderBackend[ExecutionsBackend].
    buildU

  def apply() = component()

}
