/*
 * Copyright 2015 A. Alonso Dominguez
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

import cats.syntax.show._

import diode.data.PotMap
import diode.react.ModelProxy

import io.quckoo.{TaskExecution, TaskId}
import io.quckoo.console.components._
import io.quckoo.console.core.LoadExecutions

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Created by alonsodomin on 15/05/2016.
  */
object TaskExecutionList {

  final val Columns = List('ID, 'Task, 'Status, 'Outcome)

  final case class Props(proxy: ModelProxy[PotMap[TaskId, TaskExecution]])

  class Backend($ : BackendScope[Props, Unit]) {

    def mounted(props: Props): Callback =
      Callback.when(props.proxy().size == 0)(
        props.proxy.dispatchCB(LoadExecutions))

    def renderItem(taskId: TaskId,
                   execution: TaskExecution,
                   column: Symbol): VdomNode =
      column match {
        case 'ID      => taskId.show
        case 'Task    => execution.task.show
        case 'Status  => execution.status.show
        case 'Outcome => execution.outcome.map(_.toString).getOrElse[String]("")
      }

    def render(props: Props) = {
      val model = props.proxy()
      Table(Columns, model.seq, renderItem)
    }

  }

  private[this] val component = ScalaComponent
    .builder[Props]("TaskExecutionList")
    .stateless
    .renderBackend[Backend]
    .componentDidMount($ => $.backend.mounted($.props))
    .build

  def apply(proxy: ModelProxy[PotMap[TaskId, TaskExecution]]) =
    component(Props(proxy))

}
