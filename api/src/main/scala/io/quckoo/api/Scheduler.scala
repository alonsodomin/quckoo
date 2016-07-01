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

package io.quckoo.api

import io.quckoo.ExecutionPlan
import io.quckoo.id.{PlanId, TaskId}
import io.quckoo.protocol.registry.JobNotFound
import io.quckoo.protocol.scheduler._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 13/03/2016.
  */
trait Scheduler {

  def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Option[ExecutionPlan]]

  def executionPlans(implicit ec: ExecutionContext): Future[Map[PlanId, ExecutionPlan]]

  def tasks(implicit ec: ExecutionContext): Future[Map[TaskId, TaskDetails]]

  def task(taskId: TaskId)(implicit ec: ExecutionContext): Future[Option[TaskDetails]]

  def schedule(schedule: ScheduleJob)(implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]]

}
