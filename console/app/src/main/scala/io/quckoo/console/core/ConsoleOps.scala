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

import diode.data.{Failed, Pot, Ready, Unavailable}
import io.quckoo.{ExecutionPlan, JobSpec}
import io.quckoo.client.QuckooClient
import io.quckoo.id._
import io.quckoo.net.QuckooState
import io.quckoo.protocol.scheduler.TaskDetails

import scala.concurrent.Future
import scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 25/03/2016.
  */
private[core] trait ConsoleOps {

  def refreshClusterStatus(implicit client: QuckooClient): Future[ClusterStateLoaded] =
    client.clusterState.map(ClusterStateLoaded(_))

  def loadJobSpec(jobId: JobId)(implicit client: QuckooClient): Future[(JobId, Pot[JobSpec])] =
    client.fetchJob(jobId).map {
      case Some(spec) => (jobId, Ready(spec))
      case None       => (jobId, Unavailable)
    }

  def loadJobSpecs(keys: Set[JobId] = Set.empty)(implicit client: QuckooClient): Future[Map[JobId, Pot[JobSpec]]] = {
    if (keys.isEmpty) {
      client.fetchJobs.map(_.map { case (k, v) => (k, Ready(v)) })
    } else {
      Future.sequence(keys.map(loadJobSpec)).map(_.toMap)
    }
  }

  def loadPlans(ids: Set[PlanId] = Set.empty)(implicit client: QuckooClient): Future[Map[PlanId, Pot[ExecutionPlan]]] = {
    if (ids.isEmpty) {
      client.executionPlans.map(_.map { case (k, v) => (k, Ready(v)) })
    } else {
      Future.sequence(ids.map(loadPlan)).map(_.toMap)
    }

  }

  def loadPlan(id: PlanId)(implicit client: QuckooClient): Future[(PlanId, Pot[ExecutionPlan])] =
    client.executionPlan(id).map {
      case Some(plan) => id -> Ready(plan)
      case None       => id -> Unavailable
    }

  def loadTasks(ids: Set[TaskId] = Set.empty)(implicit client: QuckooClient): Future[Map[TaskId, Pot[TaskDetails]]] = {
    if (ids.isEmpty) {
      client.tasks.map(_.map { case (k, v) => (k, Ready(v)) })
    } else {
      Future.sequence(ids.map(loadTask)).map(_.toMap)
    }
  }

  def loadTask(id: TaskId)(implicit client: QuckooClient): Future[(TaskId, Pot[TaskDetails])] =
    client.task(id).map {
      case Some(task) => id -> Ready(task)
      case None       => id -> Unavailable
    }

}
