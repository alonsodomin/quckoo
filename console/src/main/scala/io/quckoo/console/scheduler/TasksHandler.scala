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

import diode.{Effect, ModelRW}
import diode.data.{AsyncAction, PotMap}

import io.quckoo.{TaskExecution, TaskId}
import io.quckoo.console.core._

import slogging._

import scala.concurrent.ExecutionContext

/**
  * Created by alonsodomin on 14/05/2017.
  */
class TasksHandler(model: ModelRW[ConsoleScope, PotMap[TaskId, TaskExecution]], ops: ConsoleOps)(
    implicit ec: ExecutionContext
) extends ConsoleHandler[PotMap[TaskId, TaskExecution]](model) with LazyLogging {

  override protected def handle = {
    case LoadExecutions =>
      runClientIO(ops.loadTasks().map(ExecutionsLoaded))

    case ExecutionsLoaded(tasks) if tasks.nonEmpty =>
      updated(PotMap(ExecutionFetcher, tasks))

    case action: RefreshExecutions =>
      val refreshEffect = action.clientEffect(ops.loadTasks(action.keys))
      action.handleWith(this, refreshEffect)(AsyncAction.mapHandler(action.keys))
  }

}
