package io.quckoo.console.scheduler

import diode.data.PotMap
import diode.react.ModelProxy

import io.quckoo.console.components._
import io.quckoo.console.core.LoadTasks
import io.quckoo.id.TaskId
import io.quckoo.protocol.scheduler.TaskDetails

import japgolly.scalajs.react._

/**
  * Created by alonsodomin on 15/05/2016.
  */
object TaskList {

  final val Columns = List("Task ID", "Artifact ID")

  final case class Props(proxy: ModelProxy[PotMap[TaskId, TaskDetails]])

  class Backend($: BackendScope[Props, Unit]) {

    def mounted(props: Props): Callback =
      Callback.when(props.proxy().size == 0)(props.proxy.dispatch(LoadTasks))

    def renderItem(taskId: TaskId, task: TaskDetails, column: String): ReactNode = column match {
      case "Task ID"     => taskId.toString
      case "Artifact ID" => task.artifactId.toString
    }

    def render(props: Props) = {
      val model = props.proxy()
      Table(Columns, model.seq, renderItem)
    }

  }

  private[this] val component = ReactComponentB[Props]("TaskList").
    stateless.
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[PotMap[TaskId, TaskDetails]]) =
    component.withKey("task-list")(Props(proxy))

}
