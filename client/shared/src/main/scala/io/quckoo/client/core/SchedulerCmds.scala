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

package io.quckoo.client.core

import io.quckoo._
import io.quckoo.protocol.scheduler.{ExecutionPlanCancelled, ExecutionPlanStarted, ScheduleJob}

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait SchedulerCmds[P <: Protocol] {
  import CmdMarshalling.Auth

  type ScheduleJobCmd   = Auth[P, ScheduleJob, Either[JobNotFound, ExecutionPlanStarted]]
  type GetPlansCmd      = Auth[P, Unit, Seq[(PlanId, ExecutionPlan)]]
  type GetPlanCmd       = Auth[P, PlanId, Option[ExecutionPlan]]
  type GetExecutionsCmd = Auth[P, Unit, Seq[(TaskId, TaskExecution)]]
  type GetExecutionCmd  = Auth[P, TaskId, Option[TaskExecution]]
  type CancelPlanCmd    = Auth[P, PlanId, Either[ExecutionPlanNotFound, ExecutionPlanCancelled]]

  implicit def scheduleJobCmd: ScheduleJobCmd
  implicit def getPlansCmd: GetPlansCmd
  implicit def getPlanCmd: GetPlanCmd
  implicit def getExecutionsCmd: GetExecutionsCmd
  implicit def getExecutionCmd: GetExecutionCmd
  implicit def cancelPlanCmd: CancelPlanCmd
}
