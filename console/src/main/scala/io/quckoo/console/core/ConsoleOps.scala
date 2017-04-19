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

package io.quckoo.console.core

import cats.data.{EitherT, NonEmptyList}
import cats.instances.future._

import diode.data.{Pot, Ready, Unavailable}

import io.quckoo._
import io.quckoo.auth.Passport
import io.quckoo.client.http.HttpQuckooClient
import io.quckoo.protocol.Event
import io.quckoo.protocol.scheduler.ScheduleJob

import slogging.LoggerHolder

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 25/03/2016.
  */
private[core] trait ConsoleOps { this: LoggerHolder =>

  val DefaultTimeout = 2500 millis

  def refreshClusterStatus(
      implicit client: HttpQuckooClient,
      passport: Passport
  ): Future[ClusterStateLoaded] = {
    implicit val timeout = DefaultTimeout
    client.clusterState.map(ClusterStateLoaded)
  }

  def registerJob(jobSpec: JobSpec)(
      implicit client: HttpQuckooClient,
      passport: Passport
  ): Future[RegisterJobResult] = {
    implicit val timeout = 20.minutes
    client.registerJob(jobSpec).map(RegisterJobResult)
  }

  def enableJob(jobId: JobId)(
      implicit client: HttpQuckooClient,
      passport: Passport
  ): Future[Event] = {
    implicit val timeout = DefaultTimeout
    foldIntoEvent(client.enableJob(jobId))
  }

  def disableJob(jobId: JobId)(
      implicit client: HttpQuckooClient,
      passport: Passport
  ): Future[Event] = {
    implicit val timeout = DefaultTimeout
    foldIntoEvent(client.disableJob(jobId))
  }

  def loadJobSpec(jobId: JobId)(
      implicit client: HttpQuckooClient,
      passport: Passport
  ): Future[(JobId, Pot[JobSpec])] = {
    implicit val timeout = DefaultTimeout
    client.fetchJob(jobId).map {
      case Some(spec) => (jobId, Ready(spec))
      case None       => (jobId, Unavailable)
    }
  }

  def loadJobSpecs(keys: Set[JobId] = Set.empty)(
      implicit client: HttpQuckooClient,
      passport: Passport
  ): Future[Map[JobId, Pot[JobSpec]]] = {
    implicit val timeout = DefaultTimeout
    if (keys.isEmpty) {
      logger.debug("Loading all jobs from the server...")
      client.fetchJobs.map(_.map { case (k, v) => (k, Ready(v)) }.toMap)
    } else {
      logger.debug("Loading job specs for ids: {}", keys.mkString(", "))
      Future.sequence(keys.map(loadJobSpec)).map(_.toMap)
    }
  }

  def scheduleJob(details: ScheduleJob)(
      implicit client: HttpQuckooClient,
      passport: Passport
  ): Future[Event] = {
    implicit val timeout = DefaultTimeout
    foldIntoEvent(client.scheduleJob(details))
  }

  def cancelPlan(planId: PlanId)(
      implicit client: HttpQuckooClient,
      passport: Passport
  ): Future[Event] = {
    implicit val timeout = DefaultTimeout
    foldIntoEvent(client.cancelPlan(planId))
  }

  def loadPlans(ids: Set[PlanId] = Set.empty)(
      implicit client: HttpQuckooClient,
      passport: Passport
  ): Future[Map[PlanId, Pot[ExecutionPlan]]] = {
    implicit val timeout = DefaultTimeout
    if (ids.isEmpty) {
      logger.debug("Loading all execution plans from the server")
      client.executionPlans.map(_.map { case (k, v) => (k, Ready(v)) }.toMap)
    } else {
      logger.debug("Loading execution plans for ids: {}", ids.mkString(", "))
      Future.sequence(ids.map(loadPlan)).map(_.toMap)
    }
  }

  def loadPlan(id: PlanId)(
      implicit client: HttpQuckooClient,
      passport: Passport
  ): Future[(PlanId, Pot[ExecutionPlan])] = {
    implicit val timeout = DefaultTimeout
    client.executionPlan(id).map {
      case Some(plan) => id -> Ready(plan)
      case None       => id -> Unavailable
    }
  }

  def loadTasks(ids: Set[TaskId] = Set.empty)(
      implicit client: HttpQuckooClient,
      passport: Passport
  ): Future[Map[TaskId, Pot[TaskExecution]]] = {
    implicit val timeout = DefaultTimeout
    if (ids.isEmpty) {
      client.executions.map(_.map { case (k, v) => (k, Ready(v)) }.toMap)
    } else {
      Future.sequence(ids.map(loadTask)).map(_.toMap)
    }
  }

  def loadTask(id: TaskId)(
      implicit client: HttpQuckooClient,
      passport: Passport
  ): Future[(TaskId, Pot[TaskExecution])] = {
    implicit val timeout = DefaultTimeout
    client.execution(id).map {
      case Some(task) => id -> Ready(task)
      case None       => id -> Unavailable
    }
  }

  private[this] def foldIntoEvent[A <: Event](f: => Future[Either[QuckooError, A]]): Future[Event] =
    EitherT(f).leftMap(fault => Failed(NonEmptyList.of[QuckooError](fault))).fold(identity, identity)

}
