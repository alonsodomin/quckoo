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

package io.quckoo.client.ajax

import io.quckoo._
import io.quckoo.auth.User
import io.quckoo.client.QuckooClient
import io.quckoo.fault.Fault
import io.quckoo.id._
import io.quckoo.net.QuckooState
import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.serialization
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject
import org.scalajs.dom.ext.{Ajax, AjaxException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scalaz.ValidationNel

/**
  * Created by alonsodomin on 26/03/2016.
  */
private[ajax] class AjaxQuckooClient(private var authToken: Option[String]) extends QuckooClient {
  import upickle.default._
  import serialization.json.scalajs._

  private[this] def authHeaders: Map[String, String] =
    authToken.fold(Map.empty[String, String])(token => Map(AuthorizationHeader -> s"Bearer $token"))

  override def principal: Option[User] = authToken.map { token =>
    val jwtClaims = jwtDecodeClaims(token)
    User(jwtClaims("sub"))
  }

  override def close()(implicit ec: ExecutionContext): Future[Unit] =
    Ajax.post(LogoutURI, headers = authHeaders) recover { case _ => () } map { _ =>
      // Side effecting here for lacking a better way of doing this
      authToken = None
      ()
    }

  override def clusterState(implicit ec: ExecutionContext): Future[QuckooState] = {
    withAuthRefresh { () =>
      Ajax.get(ClusterStateURI, headers = authHeaders).map { xhr =>
        read[QuckooState](xhr.responseText)
      }
    }
  }

  override lazy val masterEvents: Observable[MasterEvent] =
    EventSourceObservable[MasterEvent](MasterEventsURI, "master")

  override lazy val workerEvents: Observable[WorkerEvent] =
    EventSourceObservable[WorkerEvent](WorkerEventsURI, "worker")

  override lazy val queueMetrics: Observable[TaskQueueUpdated] =
    EventSourceObservable[TaskQueueUpdated](QueueMetricsURI, "metrics")

  override def enableJob(jobId: JobId)(implicit ec: ExecutionContext): Future[JobEnabled] = {
    withAuthRefresh { () =>
      Ajax.post(JobsURI + "/" + jobId + "/enable", headers = authHeaders).map { xhr =>
        read[JobEnabled](xhr.responseText)
      }
    }
  }

  override def disableJob(jobId: JobId)(implicit ec: ExecutionContext): Future[JobDisabled] = {
    withAuthRefresh { () =>
      Ajax.post(JobsURI + "/" + jobId + "/disable", headers = authHeaders).map { xhr =>
        read[JobDisabled](xhr.responseText)
      }
    }
  }

  override def fetchJob(jobId: JobId)(implicit ec: ExecutionContext): Future[Option[JobSpec]] = {
    withAuthRefresh { () =>
      Ajax.get(JobsURI + "/" + jobId, headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
        Some(read[JobSpec](xhr.responseText))
      }
    } recover {
      case ex: AjaxException if ex.xhr.status == 404 => None
    }
  }

  override def registerJob(jobSpec: JobSpec)(implicit ec: ExecutionContext): Future[ValidationNel[Fault, JobId]] = {
    withAuthRefresh { () =>
      Ajax.put(JobsURI, write(jobSpec), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
        read[ValidationNel[Fault, JobId]](xhr.responseText)
      }
    }
  }

  override def fetchJobs(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]] = {
    withAuthRefresh { () =>
      Ajax.get(JobsURI, headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
        println(xhr.responseText)
        read[Map[JobId, JobSpec]](xhr.responseText)
      }
    }
  }

  def registryEvents: Observable[RegistryEvent] =
    EventSourceObservable[RegistryEvent](RegistryEventsURI, "RegistryEvent")

  override def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Option[ExecutionPlan]] = {
    withAuthRefresh { () =>
      Ajax.get(ExecutionPlansURI + "/" + planId, headers = authHeaders).map { xhr =>
        Some(read[ExecutionPlan](xhr.responseText))
      }
    } recover {
      case _ => None
    }
  }

  override def allExecutionPlanIds(implicit ec: ExecutionContext): Future[Set[PlanId]] = {
    withAuthRefresh { () =>
      Ajax.get(ExecutionPlansURI, headers = authHeaders).map { xhr =>
        read[Set[PlanId]](xhr.responseText)
      }
    }
  }

  override def schedule(scheduleJob: ScheduleJob)(implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    withAuthRefresh { () =>
      Ajax.post(ExecutionPlansURI, write(scheduleJob), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
        Right(read[ExecutionPlanStarted](xhr.responseText))
      }
    } recover {
      case _ => Left(JobNotFound(scheduleJob.jobId))
    }
  }

  private[this] def withAuthRefresh[A](action: () => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    action().recoverWith {
      case NonFatal(ajaxEx: AjaxException) if ajaxEx.xhr.status == 401 =>
        Ajax.get(AuthRefreshURI, headers = authHeaders).flatMap(_ => action())
    }
  }

}
