package io.quckoo.console.scheduler

import diode.data.PotMap
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.quckoo.ExecutionPlan
import io.quckoo.console.components.Notification
import io.quckoo.console.core.LoadExecutionPlans
import io.quckoo.fault.ExceptionThrown
import io.quckoo.id.PlanId
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ExecutionPlanList {

  case class Props(proxy: ModelProxy[PotMap[PlanId, ExecutionPlan]])

  class Backend($: BackendScope[Props, Unit]) {

    def mounted(props: Props): Callback = {
      def perform: Callback =
        Callback.log("Loading list of execution plans from backend...") >>
          props.proxy.dispatch(LoadExecutionPlans)

      Callback.ifTrue(props.proxy().size == 0, perform)
    }

    def render(p: Props) = {
      val model = p.proxy()
      <.table(^.`class` := "table table-striped",
        <.thead(
          <.tr(
            <.th("Job ID"),
            <.th("Plan ID"),
            <.th("Trigger"),
            <.th("Last Scheduled"),
            <.th("Last Execution"),
            <.th("Last Outcome")
          )
        ),
        <.tbody(
          model.seq.map { case (planId, schedule) => List(
            schedule.renderFailed { ex =>
              <.tr(<.td(^.colSpan := 6, Notification.danger(ExceptionThrown(ex))))
            },
            schedule.renderPending(_ > 500, _ =>
              <.tr(<.td(^.colSpan := 6, "Loading ..."))
            ),
            schedule.render { item =>
              <.tr(
                <.td(item.jobId.toString()),
                <.td(item.planId.toString()),
                <.td(item.trigger.toString()),
                <.td(item.lastScheduledTime.map(_.toString())),
                <.td(item.lastExecutionTime.map(_.toString())),
                <.td(item.lastOutcome.toString())
              )
            }
          )}
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("ExecutionPlanList").
    stateless.
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[PotMap[PlanId, ExecutionPlan]]) = component(Props(proxy))

}
