package io.quckoo.cluster

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.pattern._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.Source
import akka.util.Timeout

import io.quckoo.cluster.core._
import io.quckoo.cluster.http.HttpRouter
import io.quckoo.cluster.registry.RegistryEventPublisher
import io.quckoo.cluster.scheduler.WorkerEventPublisher
import io.quckoo.fault.Fault
import io.quckoo.id.{JobId, PlanId}
import io.quckoo.net.ClusterState
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.cluster._
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.time.TimeSource
import io.quckoo.{ExecutionPlan, JobSpec, Validated}

import org.slf4s.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

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

  def allExecutionPlanIds(implicit ec: ExecutionContext): Future[Set[PlanId]] = {
    implicit val timeout = Timeout(5 seconds)
    (core ? GetExecutionPlans).mapTo[Set[PlanId]]
  }

  def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Option[ExecutionPlan]] = {
    def internalRequest: Future[Option[ExecutionPlan]] = {
      implicit val timeout = Timeout(2 seconds)
      (core ? GetExecutionPlan(planId)).mapTo[Option[ExecutionPlan]]
    }

    implicit val sch = system.scheduler
    retry(internalRequest, 250 millis, 3)
  }

  def schedule(schedule: ScheduleJob)(implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    implicit val timeout = Timeout(5 seconds)

    (core ? schedule) map {
      case invalid: JobNotFound        => Left(invalid)
      case valid: ExecutionPlanStarted => Right(valid)
    }
  }

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
    (core ? GetJob(jobId)).mapTo[Option[JobSpec]]
  }

  def registerJob(jobSpec: JobSpec)(implicit ec: ExecutionContext): Future[Validated[JobId]] = {
    import scalaz._
    import Scalaz._

    val valid = JobSpec.validate(jobSpec)
    if (valid.isFailure) {
      Future.successful(valid.asInstanceOf[Validated[JobId]])
    } else {
      log.info(s"Registering job spec: $jobSpec")

      implicit val timeout = Timeout(30 seconds)
      (core ? RegisterJob(jobSpec)) map {
        case JobAccepted(jobId, _)  => jobId.successNel[Fault]
        case JobRejected(_, errors) => errors.failure[JobId]
      }
    }
  }

  def fetchJobs(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]] = {
    implicit val timeout = Timeout(5 seconds)
    (core ? GetJobs).mapTo[Map[JobId, JobSpec]]
  }

  def registryEvents: Source[RegistryEvent, NotUsed] =
    Source.actorPublisher[RegistryEvent](RegistryEventPublisher.props).
      mapMaterializedValue(_ => NotUsed)

  def clusterState(implicit ec: ExecutionContext): Future[ClusterState] = {
    implicit val timeout = Timeout(5 seconds)

    (core ? GetClusterStatus).mapTo[ClusterState]
  }

}
