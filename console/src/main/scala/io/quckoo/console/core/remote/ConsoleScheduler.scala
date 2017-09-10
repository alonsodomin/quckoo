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

package io.quckoo.console.core.remote

import io.quckoo.api2.Scheduler
import io.quckoo.console.core.ConsoleIO
import io.quckoo.{JobId, PlanId, TaskId, Trigger}

import scala.concurrent.duration.FiniteDuration

private[remote] trait ConsoleScheduler extends Scheduler[ConsoleIO] {
  override def cancelPlan(planId: PlanId) =
    ConsoleIO(_.cancelPlan(planId))

  override def fetchPlan(planId: PlanId) =
    ConsoleIO(_.fetchPlan(planId))

  override def allPlans =
    ConsoleIO(_.allPlans)

  override def submit(jobId: JobId, trigger: Trigger, timeout: Option[FiniteDuration]) =
    ConsoleIO(_.submit(jobId, trigger, timeout))

  override def fetchTask(taskId: TaskId) =
    ConsoleIO(_.fetchTask(taskId))

  override def allTasks =
    ConsoleIO(_.allTasks)
}
