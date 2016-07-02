package io.quckoo.console.scheduler

import diode.data.{Pot, PotMap}
import diode.react.ModelProxy
import diode.react.ReactPot._

import io.quckoo.console.components.Notification
import io.quckoo.console.core.LoadTasks
import io.quckoo.fault.ExceptionThrown
import io.quckoo.id.TaskId
import io.quckoo.protocol.scheduler.TaskDetails

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 15/05/2016.
  */
object TaskList {

  private[this] final case class TaskRowProps(taskId: TaskId, task: Pot[TaskDetails])

  private[this] val TaskRow = ReactComponentB[TaskRowProps]("TaskRow").
    stateless.
    render_P { case TaskRowProps(taskId, task) =>
      <.tr(
        task.renderFailed { ex =>
          <.td(^.colSpan := 1, Notification.danger(ExceptionThrown(ex)))
        },
        task.renderPending { _ =>
          <.td(^.colSpan := 1, "Loading ...")
        },
        task.render { item => List(
          <.td(taskId.toString()),
          <.td(item.artifactId.toString())
        )}
      )
    } build

  final case class Props(proxy: ModelProxy[PotMap[TaskId, TaskDetails]])

  class Backend($: BackendScope[Props, Unit]) {

    def mounted(props: Props): Callback =
      Callback.when(props.proxy().size == 0)(props.proxy.dispatch(LoadTasks))

    def render(props: Props) = {
      val model = props.proxy()
      <.table(^.`class` := "table table-striped",
        <.thead(
          <.tr(
            <.th("Task ID"),
            <.th("Artifact ID")
          )
        ),
        <.tbody(
          model.seq.map { case (taskId, task) =>
            TaskRow.withKey(taskId.toString)(TaskRowProps(taskId, task))
          }
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("TaskList").
    stateless.
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[PotMap[TaskId, TaskDetails]]) =
    component(Props(proxy))

}