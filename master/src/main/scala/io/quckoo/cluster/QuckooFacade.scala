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

package io.quckoo.cluster

import java.time.Clock

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.pattern._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy}
import akka.stream.scaladsl.Source
import akka.util.Timeout

import cats.data.{EitherT, ValidatedNel}
import cats.effect._
import cats.implicits._

import io.quckoo._
import io.quckoo.auth.Passport
import io.quckoo.cluster.config.ClusterSettings
import io.quckoo.cluster.core._
import io.quckoo.cluster.http.HttpRouter
import io.quckoo.cluster.journal.QuckooProductionJournal
import io.quckoo.net.QuckooState
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.cluster._
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.util._

import _root_.kamon.Kamon
import _root_.kamon.prometheus.PrometheusReporter

import slogging._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._

/**
  * Created by alonsodomin on 13/12/2015.
  */
object QuckooFacade extends LazyLogging {

  final val DefaultTimeout: FiniteDuration = 2500 millis
  final val DefaultBufferSize              = 100

  def start(settings: ClusterSettings)(implicit system: ActorSystem, clock: Clock): Future[Unit] = {
    def startHttpListener(facade: QuckooFacade)(implicit ec: ExecutionContext) = {
      implicit val materializer =
        ActorMaterializer(ActorMaterializerSettings(system), "http")

      Http()
        .bindAndHandle(
          facade.router,
          settings.http.bindInterface.value,
          settings.http.bindPort.value
        )
        .map { _ =>
          logger.info(
            s"HTTP server started on ${settings.http.bindInterface}:${settings.http.bindPort}"
          )
        }
    }

    def startMonitoring(implicit ec: ExecutionContext): Future[Unit] =
      Future(Kamon.addReporter(new PrometheusReporter))

    val promise = Promise[Unit]()
    val journal = new QuckooProductionJournal
    val guardian =
      system.actorOf(QuckooGuardian.props(settings, journal, promise), "quckoo")

    import system.dispatcher
    (promise.future, startHttpListener(new QuckooFacade(guardian)), startMonitoring)
      .mapN((_, _, _) => ())
  }

}

final class QuckooFacade(core: ActorRef)(implicit system: ActorSystem)
    extends HttpRouter with QuckooServer with LazyLogging {

  import QuckooFacade._
  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  def startPlan(schedule: ScheduleJob): IO[ExecutionPlanStarted] = IO.fromFuture {
    IO {
      implicit val to = Timeout(DefaultTimeout)
      (core ? schedule) flatMap {
        case fault: QuckooError            => Future.failed(fault)
        case started: ExecutionPlanStarted => Future.successful(started)
      }
    }
  }

  def cancelPlan(planId: PlanId): IO[ExecutionPlanCancelled] = IO.fromFuture {
    IO {
      implicit val to = Timeout(DefaultTimeout)
      (core ? CancelExecutionPlan(planId)).flatMap {
        case msg: ExecutionPlanNotFound  => Future.failed(msg)
        case msg: ExecutionPlanCancelled => Future.successful(msg)
      }
    }
  }

  def fetchPlans(): IO[List[(PlanId, ExecutionPlan)]] = IO.fromFuture {
    val executionPlans = Source
      .actorRef[(PlanId, ExecutionPlan)](bufferSize = DefaultBufferSize, OverflowStrategy.fail)
      .mapMaterializedValue(upstream => core.tell(GetExecutionPlans, upstream))

    IO(
      executionPlans
        .runFold(Map.empty[PlanId, ExecutionPlan])((map, pair) => map + pair)
        .map(_.toList)
    )
  }

  def fetchPlan(planId: PlanId): IO[Option[ExecutionPlan]] = {
    def internalRequest = IO {
      implicit val to = Timeout(DefaultTimeout)
      (core ? GetExecutionPlan(planId)).map {
        case ExecutionPlanNotFound(`planId`) => None
        case plan: ExecutionPlan             => Some(plan)
      }
    }

    /*implicit val sch = system.scheduler
    retry(internalRequest, 250 millis, 3)*/
    IO.fromFuture(internalRequest)
  }

  def fetchTasks(): IO[List[(TaskId, TaskExecution)]] = IO.fromFuture {
    val tasks = Source
      .actorRef[(TaskId, TaskExecution)](bufferSize = DefaultBufferSize, OverflowStrategy.fail)
      .mapMaterializedValue(upstream => core.tell(GetTaskExecutions, upstream))

    IO(
      tasks
        .runFold(Map.empty[TaskId, TaskExecution])((map, pair) => map + pair)
        .map(_.toList)
    )
  }

  def fetchTask(taskId: TaskId): IO[Option[TaskExecution]] = IO.fromFuture {
    IO {
      implicit val to = Timeout(DefaultTimeout)
      (core ? GetTaskExecution(taskId)) map {
        case task: TaskExecution             => Some(task)
        case TaskExecutionNotFound(`taskId`) => None
      }
    }
  }

  def schedulerTopic: Source[SchedulerEvent, NotUsed] =
    Topic.source[SchedulerEvent]

  def masterTopic: Source[MasterEvent, NotUsed] =
    Topic.source[MasterEvent]

  def workerTopic: Source[WorkerEvent, NotUsed] =
    Topic.source[WorkerEvent]

  def enableJob(jobId: JobId): IO[Unit] = IO.fromFuture {
    IO {
      implicit val to = Timeout(DefaultTimeout)
      (core ? EnableJob(jobId)).flatMap {
        case msg: JobNotFound => Future.failed(msg)
        case msg: JobEnabled  => Future.successful(())
      }
    }
  }

  def disableJob(jobId: JobId): IO[Unit] = IO.fromFuture {
    IO {
      implicit val to = Timeout(DefaultTimeout)
      (core ? DisableJob(jobId)).flatMap {
        case msg: JobNotFound => Future.failed(msg)
        case msg: JobDisabled => Future.successful(())
      }
    }
  }

  def fetchJob(jobId: JobId): IO[Option[JobSpec]] = IO.fromFuture {
    IO {
      implicit val to = Timeout(DefaultTimeout)
      (core ? GetJob(jobId)).map {
        case JobNotFound(_) => None
        case spec: JobSpec  => Some(spec)
      }
    }
  }

  def registerJob(jobSpec: JobSpec): IO[ValidatedNel[QuckooError, JobId]] = {
    val validatedJobSpec = JobSpec.valid.async
      .run(jobSpec)
      .map(_.leftMap(ValidationFault).leftMap(_.asInstanceOf[QuckooError]))

    EitherT(validatedJobSpec.map(_.toEither))
      .flatMapF { validJobSpec =>
        implicit val to = Timeout(DefaultTimeout)
        logger.info(s"Registering job spec: $validJobSpec")

        (core ? RegisterJob(validJobSpec)) map {
          case JobAccepted(jobId, _) => jobId.asRight[QuckooError]
          case JobRejected(_, error) => error.asLeft[JobId]
        }
      }
      .mapK(future2IO)
      .value
      .map(_.toValidatedNel)
  }

  def fetchJobs(): IO[List[(JobId, JobSpec)]] = IO.fromFuture {
    IO {
      Source
        .actorRef[(JobId, JobSpec)](bufferSize = DefaultBufferSize, OverflowStrategy.fail)
        .mapMaterializedValue { upstream =>
          core.tell(GetJobs, upstream)
        }
        .runFold(Map.empty[JobId, JobSpec])((map, pair) => map + pair)
        .map(_.toList)
    }
  }

  def registryTopic: Source[RegistryEvent, NotUsed] =
    Topic.source[RegistryEvent]

  def clusterState: IO[QuckooState] = IO.fromFuture {
    implicit val to = Timeout(DefaultTimeout)
    IO((core ? GetClusterStatus).mapTo[QuckooState])
  }

}
