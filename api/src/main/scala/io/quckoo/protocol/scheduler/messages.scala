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

package io.quckoo.protocol.scheduler

import io.quckoo.Task
import io.quckoo.{Task, Trigger}
import io.quckoo.Trigger.Immediate
import io.quckoo.id._

import monocle.macros.Lenses

import scala.concurrent.duration.FiniteDuration

sealed trait SchedulerMessage
sealed trait SchedulerCommand extends SchedulerMessage
sealed trait SchedulerEvent extends SchedulerMessage

@Lenses
final case class ScheduleJob(
    jobId: JobId,
    //params: Map[String, String] = Map.empty,
    trigger: Trigger = Immediate,
    timeout: Option[FiniteDuration] = None
) extends SchedulerCommand

final case class TaskScheduled(jobId: JobId, planId: PlanId, taskId: TaskId) extends SchedulerEvent
final case class TaskTriggered(jobId: JobId, planId: PlanId, taskId: TaskId) extends SchedulerEvent
final case class TaskCompleted(jobId: JobId, planId: PlanId, taskId: TaskId, outcome: Task.Outcome) extends SchedulerEvent

final case class JobNotEnabled(jobId: JobId) extends SchedulerMessage
final case class JobFailedToSchedule(jobId: JobId, cause: Throwable) extends SchedulerMessage

final case class ExecutionPlanStarted(jobId: JobId, planId: PlanId) extends SchedulerEvent
final case class ExecutionPlanFinished(jobId: JobId, planId: PlanId) extends SchedulerEvent

case object GetExecutionPlans extends SchedulerCommand
final case class GetExecutionPlan(planId: PlanId) extends SchedulerCommand
final case class ExecutionPlanNotFound(planId: PlanId) extends SchedulerEvent
final case class CancelPlan(planId: PlanId) extends SchedulerCommand

final case class TaskDetails(artifactId: ArtifactId, jobClass: String, outcome: Task.Outcome)
case object GetTasks extends SchedulerCommand
final case class GetTask(taskId: TaskId) extends SchedulerCommand
final case class TaskNotFound(taskId: TaskId) extends SchedulerEvent

final case class TaskQueueUpdated(pendingTasks: Int, inProgressTasks: Int) extends SchedulerEvent
