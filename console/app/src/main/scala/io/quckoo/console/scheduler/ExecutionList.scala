package io.quckoo.console.scheduler

import diode.data.{PotMap, PotVector}
import diode.react.ModelProxy
import io.quckoo.console.core.TaskItem
import io.quckoo.id.TaskId
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 15/05/2016.
  */
object ExecutionList {

  final case class Props(proxy: ModelProxy[PotMap[TaskId, TaskItem]])

  class Backend($: BackendScope[Props, Unit]) {

    def render(props: Props) = {
      <.div("Hello")
    }

  }

  private[this] val component = ReactComponentB[Props]("TaskList").
    stateless.
    renderBackend[Backend].
    build

  def apply(proxy: ModelProxy[PotMap[TaskId, TaskItem]]) =
    component(Props(proxy))

}
