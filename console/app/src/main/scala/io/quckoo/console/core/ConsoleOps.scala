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

import diode.data.{Pot, Ready, Unavailable}
import io.quckoo.auth.Passport
import io.quckoo.client.http.HttpQuckooClient
import io.quckoo.id._
import io.quckoo.protocol.Event
import io.quckoo.protocol.registry.RegistryEvent
import io.quckoo.protocol.scheduler.{ScheduleJob, SchedulerEvent}
import io.quckoo.{ExecutionPlan, JobSpec, TaskExecution}
import slogging.LoggerHolder

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 25/03/2016.
  */
private[core] trait ConsoleOps { this: LoggerHolder =>

  implicit val DefaultTimeout = 2500 millis

  def refreshClusterStatus(
    implicit
    client: HttpQuckooClient, passport: Passport
  ): Future[ClusterStateLoaded] = {
    client.clusterState.map(ClusterStateLoaded)
  }

  def registerJob(jobSpec: JobSpec)(
    implicit
    client: HttpQuckooClient, passport: Passport
  ): Future[RegisterJobResult] = {
    client.registerJob(jobSpec).map(RegisterJobResult)
  }

  def enableJob(jobId: JobId)(
    implicit
    client: HttpQuckooClient, passport: Passport
  ): Future[RegistryEvent] = {
    EitherT(client.enableJob(jobId)).fold(identity, identity)
  }

  def disableJob(jobId: JobId)(
    implicit
    client: HttpQuckooClient, passport: Passport
  ): Future[RegistryEvent] = {
    EitherT(client.disableJob(jobId)).fold(identity, identity)
  }

  def loadJobSpec(jobId: JobId)(
    implicit
    client: HttpQuckooClient, passport: Passport
  ): Future[(JobId, Pot[JobSpec])] = {
    client.fetchJob(jobId).map {
      case Some(spec) => (jobId, Ready(spec))
      case None => (jobId, Unavailable)
    }
  }

  def loadJobSpecs(keys: Set[JobId] = Set.empty)(
    implicit
    client: HttpQuckooClient, passport: Passport
  ): Future[Map[JobId, Pot[JobSpec]]] = {
    if (keys.isEmpty) {
      client.fetchJobs.map(_.map { case (k, v) => (k, Ready(v)) })
    } else {
      Future.sequence(keys.map(loadJobSpec)).map(_.toMap)
    }
  }

  def scheduleJob(details: ScheduleJob)(
    implicit
    client: HttpQuckooClient, passport: Passport
  ): Future[Event] = {
    EitherT(client.scheduleJob(details)).fold(identity, identity)
  }

  def cancelPlan(planId: PlanId)(
    implicit
    client: HttpQuckooClient, passport: Passport
  ): Future[Event] = {
    EitherT(client.cancelExecutionPlan(planId)).map(_ => ExecutionPlanCancelled(planId)).fold(identity, identity)
  }

  def loadPlans(ids: Set[PlanId] = Set.empty)(
    implicit
    client: HttpQuckooClient, passport: Passport
  ): Future[Map[PlanId, Pot[ExecutionPlan]]] = {
    if (ids.isEmpty) {
      client.executionPlans.map(_.map { case (k, v) => (k, Ready(v)) })
    } else {
      Future.sequence(ids.map(loadPlan)).map(_.toMap)
    }
  }

  def loadPlan(id: PlanId)(
    implicit
    client: HttpQuckooClient, passport: Passport
  ): Future[(PlanId, Pot[ExecutionPlan])] = {
    client.executionPlan(id).map {
      case Some(plan) => id -> Ready(plan)
      case None       => id -> Unavailable
    }
  }

  def loadTasks(ids: Set[TaskId] = Set.empty)(
    implicit
    client: HttpQuckooClient, passport: Passport
  ): Future[Map[TaskId, Pot[TaskExecution]]] = {
    if (ids.isEmpty) {
      client.executions.map(_.map { case (k, v) => (k, Ready(v)) })
    } else {
      Future.sequence(ids.map(loadTask)).map(_.toMap)
    }
  }

  def loadTask(id: TaskId)(
    implicit
    client: HttpQuckooClient, passport: Passport
  ): Future[(TaskId, Pot[TaskExecution])] = {
    client.execution(id).map {
      case Some(task) => id -> Ready(task)
      case None       => id -> Unavailable
    }
  }

}
