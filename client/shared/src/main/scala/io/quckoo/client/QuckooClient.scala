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

package io.quckoo.client

import cats.data.ValidatedNel

import io.quckoo._
import io.quckoo.api.{Cluster, Registry, Scheduler}
import io.quckoo.auth.{Credentials, Passport}
import io.quckoo.client.core._
import io.quckoo.net.QuckooState
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._

import monix.reactive.Observable

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 10/09/2016.
  */
object QuckooClient {

  def apply[P <: Protocol](implicit driver: Driver[P]): QuckooClient[P] =
    new QuckooClient[P](driver)

}

final class QuckooClient[P <: Protocol] private[client] (driver: Driver[P])
    extends Cluster with Registry with Scheduler {
  import driver.specs._

  def channel[E](implicit magnet: ChannelMagnet[E]): Observable[E] = {
    val channelDef = magnet.resolve(driver)
    driver.openChannel[E](channelDef).run(())
  }

  // -- Security

  def authenticate(username: String, password: String)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration
  ): Future[Passport] = {
    val cmd = AnonCmd(Credentials(username, password), timeout)
    driver.invoke[AuthenticateCmd].run(cmd)
  }

  def refreshPassport(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Passport] =
    driver.invoke[RefreshPassportCmd].run(AuthCmd((), timeout, passport))

  def signOut(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Unit] =
    driver.invoke[SingOutCmd].run(AuthCmd((), timeout, passport))

  // -- Cluster

  def clusterState(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[QuckooState] = {
    val cmd = AuthCmd((), timeout, passport)
    driver.invoke[GetClusterStateCmd].run(cmd)
  }

  // -- Registry

  def registerJob(job: JobSpec)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[ValidatedNel[QuckooError, JobId]] = {
    val cmd = AuthCmd(job, timeout, passport)
    driver.invoke[RegisterJobCmd].run(cmd)
  }

  def fetchJob(jobId: JobId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[JobSpec]] = {
    val cmd = AuthCmd(jobId, timeout, passport)
    driver.invoke[GetJobCmd].run(cmd)
  }

  def fetchJobs(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[List[(JobId, JobSpec)]] = {
    val cmd = AuthCmd((), timeout, passport)
    driver.invoke[GetJobsCmd].run(cmd)
  }

  def enableJob(jobId: JobId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Either[JobNotFound, JobEnabled]] = {
    val cmd = AuthCmd(jobId, timeout, passport)
    driver.invoke[EnableJobCmd].run(cmd)
  }

  def disableJob(jobId: JobId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Either[JobNotFound, JobDisabled]] = {
    val cmd = AuthCmd(jobId, timeout, passport)
    driver.invoke[DisableJobCmd].run(cmd)
  }

  // -- Scheduler

  def executionPlans(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Seq[(PlanId, ExecutionPlan)]] = {
    val cmd = AuthCmd((), timeout, passport)
    driver.invoke[GetPlansCmd].run(cmd)
  }

  def executionPlan(planId: PlanId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[ExecutionPlan]] = {
    val cmd = AuthCmd(planId, timeout, passport)
    driver.invoke[GetPlanCmd].run(cmd)
  }

  def scheduleJob(schedule: ScheduleJob)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    val cmd = AuthCmd(schedule, timeout, passport)
    driver.invoke[ScheduleJobCmd].run(cmd)
  }

  def cancelPlan(planId: PlanId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Either[ExecutionPlanNotFound, ExecutionPlanCancelled]] = {
    val cmd = AuthCmd(planId, timeout, passport)
    driver.invoke[CancelPlanCmd].run(cmd)
  }

  def executions(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Seq[(TaskId, TaskExecution)]] = {
    val cmd = AuthCmd((), timeout, passport)
    driver.invoke[GetExecutionsCmd].run(cmd)
  }

  def execution(taskId: TaskId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[TaskExecution]] = {
    val cmd = AuthCmd(taskId, timeout, passport)
    driver.invoke[GetExecutionCmd].run(cmd)
  }

}
