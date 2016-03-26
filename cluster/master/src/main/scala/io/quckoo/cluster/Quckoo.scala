package io.quckoo.cluster

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import de.heikoseeberger.akkasse.ServerSentEvent
import io.quckoo.auth.AuthInfo
import io.quckoo.cluster.core._
import io.quckoo.cluster.http.HttpRouter
import io.quckoo.cluster.protocol.GetClusterStatus
import io.quckoo.fault.Fault
import io.quckoo.id.{JobId, PlanId}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.cluster._
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
    extends HttpRouter with Server with Logging with Retrying {

  import UserAuthenticator._

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

  def events = Source.actorPublisher[KairosClusterEvent](KairosEventEmitter.props).
    map(evt => {
      import upickle.default._
      ServerSentEvent(write[KairosClusterEvent](evt))
    })

  def clusterDetails(implicit ec: ExecutionContext): Future[ClusterInfo] = {
    implicit val timeout = Timeout(5 seconds)

    (core ? GetClusterStatus).mapTo[KairosStatus] map { status =>
      val nodeInfo = NodeInfo(status.members.size)
      ClusterInfo(nodeInfo, 0)
    }
  }

  def authenticate(username: String, password: Array[Char])(implicit ec: ExecutionContext): Future[Option[AuthInfo]] = {
    implicit val timeout = Timeout(5 seconds)

    userAuth ? Authenticate(username, password) map {
      case AuthenticationSuccess(authInfo) => Some(authInfo)
      case AuthenticationFailed            => None
    }
  }

}
