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

package io.quckoo.console.core

import cats.data.{EitherT, NonEmptyList}
import cats.implicits._

import diode.data.{Pot, Ready, Unavailable}

import io.quckoo._
import io.quckoo.auth.Passport
import io.quckoo.client.ClientIO
import io.quckoo.client.http.HttpQuckooClient
import io.quckoo.protocol.Event
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._

import slogging.LoggerHolder

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 25/03/2016.
  */
trait ConsoleOps { this: LoggerHolder =>

  protected def client: HttpQuckooClient

  def refreshClusterStatus: ClientIO[ClusterStateLoaded] = {
    logger.debug("Refreshing cluster status...")
    client.clusterState.map(ClusterStateLoaded)
  }

  def registerJob(jobSpec: JobSpec): ClientIO[RegisterJobResult] =
    client.registerJob(jobSpec).map(RegisterJobResult)

  def enableJob(jobId: JobId): ClientIO[JobEnabled] =
    client.enableJob(jobId).map(_ => JobEnabled(jobId))

  def disableJob(jobId: JobId): ClientIO[JobDisabled] =
    client.disableJob(jobId).map(_ => JobDisabled(jobId))

  def loadJobSpec(jobId: JobId): ClientIO[(JobId, Pot[JobSpec])] =
    client.fetchJob(jobId).map {
      case Some(spec) => (jobId, Ready(spec))
      case None       => (jobId, Unavailable)
    }

  def loadJobSpecs(keys: Set[JobId] = Set.empty): ClientIO[Map[JobId, Pot[JobSpec]]] =
    if (keys.isEmpty) {
      logger.debug("Loading all jobs from the server...")
      client.fetchJobs.map(_.map { case (k, v) => (k, Ready(v)) }.toMap)
    } else {
      logger.debug("Loading job specs for ids: {}", keys.mkString(", "))
      keys.toList.traverse(loadJobSpec).map(_.toMap)
    }

  def scheduleJob(details: ScheduleJob): ClientIO[ExecutionPlanStarted] =
    client.startPlan(details)

  def cancelPlan(planId: PlanId): ClientIO[ExecutionPlanCancelled] =
    client.cancelPlan(planId)

  def loadPlans(ids: Set[PlanId] = Set.empty): ClientIO[Map[PlanId, Pot[ExecutionPlan]]] =
    if (ids.isEmpty) {
      logger.debug("Loading all execution plans from the server")
      client.fetchPlans.map(_.map { case (k, v) => (k, Ready(v)) }.toMap)
    } else {
      logger.debug("Loading execution plans for ids: {}", ids.mkString(", "))
      ids.toList.traverse(loadPlan).map(_.toMap)
    }

  def loadPlan(id: PlanId): ClientIO[(PlanId, Pot[ExecutionPlan])] =
    client.fetchPlan(id).map {
      case Some(plan) => id -> Ready(plan)
      case None       => id -> Unavailable
    }

  def loadTasks(ids: Set[TaskId] = Set.empty): ClientIO[Map[TaskId, Pot[TaskExecution]]] =
    if (ids.isEmpty) {
      client.fetchTasks().map(_.map { case (k, v) => (k, Ready(v)) }.toMap)
    } else {
      ids.toList.traverse(loadTask).map(_.toMap)
    }

  def loadTask(id: TaskId): ClientIO[(TaskId, Pot[TaskExecution])] =
    client.fetchTask(id).map {
      case Some(task) => id -> Ready(task)
      case None       => id -> Unavailable
    }

}
