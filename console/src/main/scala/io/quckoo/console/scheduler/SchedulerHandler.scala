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

import io.quckoo.console.components.Notification
import io.quckoo.console.core._
import io.quckoo.protocol.scheduler._

import slogging._

import scala.concurrent.ExecutionContext

/**
  * Created by alonsodomin on 14/05/2017.
  */
class SchedulerHandler(model: ModelRW[ConsoleScope, UserScope], ops: ConsoleOps)(
    implicit ec: ExecutionContext
) extends ConsoleHandler[UserScope](model) with AuthHandler[UserScope] with LazyLogging {

  override def handle = {
    case msg: ScheduleJob =>
      withAuth { implicit passport =>
        effectOnly(Effect(ops.scheduleJob(msg)))
      }

    case CancelExecutionPlan(planId) =>
      withAuth { implicit passport =>
        effectOnly(Effect(ops.cancelPlan(planId)))
      }

    case ExecutionPlanStarted(jobId, planId, _) =>
      val effect = Effects.parallel(
        Growl(Notification.success(s"Started execution plan for job. planId=$planId")),
        RefreshExecutionPlans(Set(planId))
      )
      effectOnly(effect)

    case ExecutionPlanFinished(jobId, planId, _) =>
      effectOnly(RefreshExecutionPlans(Set(planId)))

    case ExecutionPlanCancelled(_, planId, _) =>
      val effects = Effects.parallel(
        Growl(Notification.success(s"Execution plan $planId has been cancelled")),
        RefreshExecutionPlans(Set(planId))
      )
      effectOnly(effects)

    case TaskScheduled(_, _, task, _) =>
      effectOnly(RefreshExecutions(Set(task.id)))

    case TaskTriggered(_, _, taskId, _) =>
      effectOnly(RefreshExecutions(Set(taskId)))

    case TaskCompleted(_, _, taskId, _, _) =>
      effectOnly(RefreshExecutions(Set(taskId)))
  }

}
