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
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.pattern._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy}
import akka.stream.scaladsl.Source
import akka.util.Timeout
import io.quckoo.cluster.core._
import io.quckoo.cluster.http.HttpRouter
import io.quckoo.cluster.registry.RegistryEventPublisher
import io.quckoo.cluster.scheduler.SchedulerEventPublisher
import io.quckoo.fault.Fault
import io.quckoo.id.{JobId, PlanId, TaskId}
import io.quckoo.net.QuckooState
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.cluster._
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.time.TimeSource
import io.quckoo.{ExecutionPlan, JobSpec, TaskExecution}
import org.slf4s.Logging

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scalaz._

/**
  * Created by alonsodomin on 13/12/2015.
  */
object QuckooFacade extends Logging {
  import Scalaz._

  def start(settings: QuckooClusterSettings)(implicit system: ActorSystem, timeSource: TimeSource): Future[Unit] = {
    def startHttpListener(facade: QuckooFacade)(implicit ec: ExecutionContext) = {
      implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system), "http")

      Http().bindAndHandle(facade.router, settings.httpInterface, settings.httpPort).
        map(_ => log.info(s"HTTP server started on ${settings.httpInterface}:${settings.httpPort}"))
    }

    log.info("Starting Quckoo...")

    val promise = Promise[Unit]()
    val guardian = system.actorOf(QuckooGuardian.props(settings, promise), "quckoo")

    import system.dispatcher
    (promise.future |@| startHttpListener(new QuckooFacade(guardian)))((_, _) => ())
  }

}

final class QuckooFacade(core: ActorRef)
                        (implicit system: ActorSystem, timeSource: TimeSource)
    extends HttpRouter with QuckooServer with Logging with Retrying {

  implicit val materializer = ActorMaterializer()

  def cancelPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Unit] = {
    implicit val timeout = Timeout(3 seconds)
    (core ? CancelExecutionPlan(planId)).mapTo[ExecutionPlanFinished].map(_ => ())
  }

  def executionPlans(implicit ec: ExecutionContext): Future[Map[PlanId, ExecutionPlan]] = {
    val executionPlans = Source.actorRef[(PlanId, ExecutionPlan)](100, OverflowStrategy.fail).
      mapMaterializedValue(upstream => core.tell(GetExecutionPlans, upstream))

    executionPlans.runFold(Map.empty[PlanId, ExecutionPlan])((map, pair) => map + pair)
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

  def executions(implicit ec: ExecutionContext): Future[Map[TaskId, TaskExecution]] = {
    val tasks = Source.actorRef[(TaskId, TaskExecution)](100, OverflowStrategy.fail).
      mapMaterializedValue(upstream => core.tell(GetTaskExecutions, upstream))
    tasks.runFold(Map.empty[TaskId, TaskExecution])((map, pair) => map + pair)
  }

  def execution(taskId: TaskId)(implicit ec: ExecutionContext): Future[Option[TaskExecution]] = {
    implicit val timeout = Timeout(5 seconds)

    (core ? GetTaskExecution(taskId)) map {
      case task: TaskExecution             => Some(task)
      case TaskExecutionNotFound(`taskId`) => None
    }
  }

  def schedule(schedule: ScheduleJob)(implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    implicit val timeout = Timeout(5 seconds)

    (core ? schedule) map {
      case invalid: JobNotFound        => Left(invalid)
      case valid: ExecutionPlanStarted => Right(valid)
    }
  }

  def schedulerEvents: Source[SchedulerEvent, NotUsed] =
    Source.actorPublisher[SchedulerEvent](SchedulerEventPublisher.props).
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
      case JobNotFound(_) => None
      case spec: JobSpec  => Some(spec)
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
        case JobAccepted(jobId, _) => jobId.successNel[Fault]
        case JobRejected(_, error) => error.failureNel[JobId]
      }
    }
  }

  def fetchJobs(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]] = {
    Source.actorRef[(JobId, JobSpec)](100, OverflowStrategy.fail).
      mapMaterializedValue { upstream =>
        core.tell(GetJobs, upstream)
      }.runFold(Map.empty[JobId, JobSpec]) { (map, pair) =>
        map + pair
      }
  }

  def registryEvents: Source[RegistryEvent, NotUsed] =
    Source.actorPublisher[RegistryEvent](RegistryEventPublisher.props).
      mapMaterializedValue(_ => NotUsed)

  def clusterState(implicit ec: ExecutionContext): Future[QuckooState] = {
    implicit val timeout = Timeout(5 seconds)

    (core ? GetClusterStatus).mapTo[QuckooState]
  }

}
