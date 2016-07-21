/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  final val Columns = List("Task ID", "Outcome")

  final case class Props(proxy: ModelProxy[PotMap[TaskId, TaskDetails]])

  class Backend($: BackendScope[Props, Unit]) {

    def mounted(props: Props): Callback =
      Callback.when(props.proxy().size == 0)(props.proxy.dispatch(LoadTasks))

    def renderItem(taskId: TaskId, task: TaskDetails, column: String): ReactNode = column match {
      case "Task ID" => taskId.toString
      case "Outcome" => task.outcome.toString
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
