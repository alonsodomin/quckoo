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

package io.quckoo.client

import cats.data._

import io.quckoo._
import io.quckoo.auth.Passport
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._

trait NewQuckooClient {

  def signIn(username: String, password: String): ClientIO[Unit]
  def signOut(): ClientIO[Unit]

  // -- Registry

  def registerJob(jobSpec: JobSpec): ClientIO[ValidatedNel[QuckooError, JobId]]
  def fetchJob(jobId: JobId): ClientIO[Option[JobSpec]]
  def fetchJobs(): ClientIO[Seq[(JobId, JobSpec)]]
  def enableJob(jobId: JobId): ClientIO[Either[JobNotFound, JobEnabled]]
  def disableJob(jobId: JobId): ClientIO[Either[JobNotFound, JobDisabled]]

  // -- Scheduler

  def startPlan(schedule: ScheduleJob): ClientIO[Either[JobNotFound, ExecutionPlanStarted]]
  def cancelPlan(planId: PlanId): ClientIO[Either[ExecutionPlanNotFound, ExecutionPlanCancelled]]
  def fetchPlans(): ClientIO[Seq[(PlanId, ExecutionPlan)]]
  def fetchPlan(planId: PlanId): ClientIO[Option[ExecutionPlan]]
  def fetchTasks(): ClientIO[Seq[(TaskId, TaskExecution)]]
  def fetchTask(taskId: TaskId): ClientIO[Option[TaskExecution]]
}

object NewQuckooClient {
  case class ClientState(passport: Option[Passport])
}