package io.kairos.console.client.scheduler

import diode.data.PotMap
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.kairos.console.client.core.LoadSchedules
import io.kairos.console.model.Schedule
import io.kairos.fault.ExceptionThrown
import io.kairos.id.PlanId
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object SchedulesList {

  case class Props(proxy: ModelProxy[PotMap[PlanId, Schedule]])

  class Backend($: BackendScope[Props, Unit]) {

    def mounted(props: Props): Callback = {
      def perform: Callback =
        Callback.log("Loading list of schedules from backend...") >>
          props.proxy.dispatch(LoadSchedules)

      Callback.ifTrue(props.proxy().size == 0, perform)
    }

    def render(p: Props) = {
      val model = p.proxy()
      <.table(^.`class` := "table table-striped",
        <.thead(
          <.tr(
            <.th("Job ID"),
            <.th("Plan ID")
          )
        ),
        <.tbody(
          model.seq.map { case (planId, schedule) =>
            <.tr(^.key := planId.toString(),
              schedule.renderFailed { ex =>
                <.td(^.colSpan := 4, ExceptionThrown(ex).toString())
              },
              schedule.renderPending(_ > 500, _ => "Loading ..."),
              schedule.render { item => List(
                <.td(item.jobId.toString()),
                <.td(item.planId.toString())
              )}
            )
          }
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("SchedulesList").
    stateless.
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[PotMap[PlanId, Schedule]]) = component(Props(proxy))

}
