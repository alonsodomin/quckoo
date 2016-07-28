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

import io.quckoo.TaskExecution
import io.quckoo.console.components._
import io.quckoo.console.core.LoadExecutions
import io.quckoo.id.TaskId

import japgolly.scalajs.react._

/**
  * Created by alonsodomin on 15/05/2016.
  */
object TaskExecutionList {

  final val Columns = List("Task ID", "Artifact", "Job Class", "Status", "Outcome")

  final case class Props(proxy: ModelProxy[PotMap[TaskId, TaskExecution]])

  class Backend($: BackendScope[Props, Unit]) {

    def mounted(props: Props): Callback =
      Callback.when(props.proxy().size == 0)(props.proxy.dispatch(LoadExecutions))

    def renderItem(taskId: TaskId, execution: TaskExecution, column: String): ReactNode = column match {
      case "Task ID"   => taskId.toString
      case "Artifact"  => execution.task.artifactId.toString
      case "Job Class" => execution.task.jobClass
      case "Status"    => execution.status.toString
      case "Outcome"   => execution.outcome.map(_.toString).getOrElse[String]("")
    }

    def render(props: Props) = {
      val model = props.proxy()
      Table(Columns, model.seq, renderItem)
    }

  }

  private[this] val component = ReactComponentB[Props]("TaskExecutionList").
    stateless.
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[PotMap[TaskId, TaskExecution]]) =
    component.withKey("task-execution-list")(Props(proxy))

}
