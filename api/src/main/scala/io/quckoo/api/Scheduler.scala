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

import io.quckoo.auth.Passport
import io.quckoo._
import io.quckoo.protocol.scheduler._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 13/03/2016.
  */
trait Scheduler {

  def cancelPlan(planId: PlanId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Either[ExecutionPlanNotFound, ExecutionPlanCancelled]]

  def executionPlan(planId: PlanId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[ExecutionPlan]]

  def executionPlans(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Seq[(PlanId, ExecutionPlan)]]

  def executions(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Seq[(TaskId, TaskExecution)]]

  def execution(taskId: TaskId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[TaskExecution]]

  def scheduleJob(schedule: ScheduleJob)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Either[QuckooError, ExecutionPlanStarted]]

}
