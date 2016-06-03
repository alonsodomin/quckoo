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

package io.quckoo.cluster

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.Message
import akka.pattern._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Source}
import akka.util.Timeout
import io.quckoo.cluster.core.{WorkerEventPublisher, _}
import io.quckoo.cluster.http.HttpRouter
import io.quckoo.cluster.registry.RegistryEventPublisher
import io.quckoo.cluster.scheduler.TaskQueueEventPublisher
import io.quckoo.fault.{Fault, ResolutionFault}
import io.quckoo.id.{JobId, PlanId, TaskId}
import io.quckoo.net.QuckooState
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.cluster._
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.time.TimeSource
import io.quckoo.{ExecutionPlan, JobSpec}
import org.slf4s.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scalaz.{Validation, ValidationNel}

/**
  * Created by alonsodomin on 13/12/2015.
  */
class Quckoo(settings: QuckooClusterSettings)
            (implicit system: ActorSystem, materializer: ActorMaterializer, timeSource: TimeSource)
    extends HttpRouter with QuckooServer with Logging with Retrying {

  val core = system.actorOf(QuckooCluster.props(settings), "quckoo")
  val userAuth = system.actorSelection(core.path / "authenticator")

  def start(implicit ec: ExecutionContext, timeout: Timeout): Future[Unit] = {
    Http().bindAndHandle(router, settings.httpInterface, settings.httpPort).
      map(_ => log.info(s"HTTP server started on ${settings.httpInterface}:${settings.httpPort}"))
  }

  def executionPlans(implicit ec: ExecutionContext): Future[Map[PlanId, ExecutionPlan]] = {
    implicit val timeout = Timeout(3 seconds)
    (core ? GetExecutionPlans).mapTo[Map[PlanId, ExecutionPlan]]
  }

  def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Option[ExecutionPlan]] = {
    def internalRequest: Future[Option[ExecutionPlan]] = {
      implicit val timeout = Timeout(2 seconds)
      (core ? GetExecutionPlan(planId)).map {
        case ExecutionPlanNotFound(`planId`) => None
        case plan: ExecutionPlan             => Some(plan)
      }
    }

    implicit val sch = system.scheduler
    retry(internalRequest, 250 millis, 3)
  }

  def tasks(implicit ec: ExecutionContext): Future[Map[TaskId, TaskDetails]] = {
    implicit val timeout = Timeout(5 seconds)

    (core ? GetTasks).mapTo[Map[TaskId, TaskDetails]]
  }

  def task(taskId: TaskId)(implicit ec: ExecutionContext): Future[Option[TaskDetails]] = {
    implicit val timeout = Timeout(5 seconds)

    (core ? GetTask(taskId)).mapTo[Option[TaskDetails]]
  }

  def schedule(schedule: ScheduleJob)(implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    implicit val timeout = Timeout(5 seconds)

    (core ? schedule) map {
      case invalid: JobNotFound        => Left(invalid)
      case valid: ExecutionPlanStarted => Right(valid)
    }
  }

  def queueMetrics: Source[TaskQueueUpdated, NotUsed] =
    Source.actorPublisher[TaskQueueUpdated](Props(classOf[TaskQueueEventPublisher])).
      mapMaterializedValue(_ => NotUsed)

  def masterEvents: Source[MasterEvent, NotUsed] =
    Source.actorPublisher[MasterEvent](MasterEventPublisher.props).
      mapMaterializedValue(_ => NotUsed)

  def workerEvents: Source[WorkerEvent, NotUsed] =
    Source.actorPublisher[WorkerEvent](WorkerEventPublisher.props).
      mapMaterializedValue(_ => NotUsed)

  def enableJob(jobId: JobId)(implicit ec: ExecutionContext): Future[JobEnabled] = {
    implicit val timeout = Timeout(5 seconds)
    (core ? EnableJob(jobId)).mapTo[JobEnabled]
  }

  def disableJob(jobId: JobId)(implicit ec: ExecutionContext): Future[JobDisabled] = {
    implicit val timeout = Timeout(5 seconds)
    (core ? DisableJob(jobId)).mapTo[JobDisabled]
  }

  def fetchJob(jobId: JobId)(implicit ec: ExecutionContext): Future[Option[JobSpec]] = {
    implicit val timeout = Timeout(5 seconds)
    (core ? GetJob(jobId)).map {
      case JobNotFound(_)      => None
      case (_, spec: JobSpec)  => Some(spec)
    }
  }

  def registerJob(jobSpec: JobSpec)(implicit ec: ExecutionContext): Future[ValidationNel[Fault, JobId]] = {
    import scalaz._
    import Scalaz._

    val valid = JobSpec.validate(jobSpec)
    if (valid.isFailure) {
      Future.successful(valid.asInstanceOf[ValidationNel[Fault, JobId]])
    } else {
      log.info(s"Registering job spec: $jobSpec")

      implicit val timeout = Timeout(30 seconds)
      (core ? RegisterJob(jobSpec)) map {
        case JobAccepted(jobId, _)  => jobId.successNel[Fault]
        case JobRejected(_, _, errors) => errors.map(_.asInstanceOf[Fault]).failure[JobId]
      }
    }
  }

  def fetchJobs(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]] = {
    implicit val timeout = Timeout(15 seconds)
    (core ? GetJobs).mapTo[Map[JobId, JobSpec]]
  }

  def registryEvents: Source[RegistryEvent, NotUsed] =
    Source.actorPublisher[RegistryEvent](RegistryEventPublisher.props).
      mapMaterializedValue(_ => NotUsed)

  def clusterState(implicit ec: ExecutionContext): Future[QuckooState] = {
    implicit val timeout = Timeout(5 seconds)

    (core ? GetClusterStatus).mapTo[QuckooState]
  }

}
