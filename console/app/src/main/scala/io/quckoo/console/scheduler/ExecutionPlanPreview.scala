package io.quckoo.console.scheduler

import io.quckoo.Trigger
import io.quckoo.time.DateTime
import io.quckoo.time.MomentJSTimeSource.Implicits.default

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 09/04/2016.
  */
object ExecutionPlanPreview {

  case class Props(trigger: Trigger)
  case class State(maxRows: Int)

  class Backend($: BackendScope[Props, State]) {

    def generateTimeline(props: Props, state: State): Seq[DateTime] = {
      import Trigger._

      def genNext(prev: ReferenceTime): (ReferenceTime, Boolean) = {
        props.trigger.nextExecutionTime(prev).
          map(next => (LastExecutionTime(next), true)).
          getOrElse((prev, false))
      }

      val first = genNext(ScheduledTime(default.currentDateTime))
      val stream = Stream.iterate(first) { case (prev, _) => genNext(prev) }
      stream.takeWhile { case (_, continue) => continue } map(_._1.when.toLocal) take state.maxRows
    }

    def onRowsSelectionUpdate(evt: ReactEventI): Callback =
      $.modState(_.copy(maxRows = evt.target.value.toInt))

    def render(props: Props, state: State) = {
      <.div(
        <.div(^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2", "Trigger"),
          <.div(^.`class` := "col-sm-10", props.trigger.toString())
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2", "Max Rows"),
          <.div(^.`class` := "col-sm-2",
            <.select(^.id := "previewRows",
              ^.`class` := "form-control",
              ^.value := state.maxRows,
              ^.onChange ==> onRowsSelectionUpdate,
              <.option(^.value := 10, "10"),
              <.option(^.value := 25, "25"),
              <.option(^.value := 50, "50")
            )
          )
        ),
        <.table(^.`class` := "table table-striped",
          <.thead(
            <.tr(<.th("Expected Executions"))
          ),
          <.tbody(
            generateTimeline(props, state).map { time =>
              <.tr(<.td(time.format("dddd, MMMM Do YYYY, h:mm:ss a")))
            }
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("ExecutionPlanPreview").
    initialState(State(10)).
    renderBackend[Backend].
    build

  def apply(trigger: Trigger) =
    component(Props(trigger))

}
