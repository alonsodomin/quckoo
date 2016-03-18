package io.kairos.cluster

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import de.heikoseeberger.akkasse.ServerSentEvent
import io.kairos.cluster.core._
import io.kairos.cluster.protocol.GetClusterStatus
import io.kairos.cluster.scheduler.ExecutionPlan
import io.kairos.console.auth.AuthInfo
import io.kairos.console.info.{ClusterInfo, NodeInfo}
import io.kairos.console.model.Schedule
import io.kairos.console.server.ServerFacade
import io.kairos.console.server.http.HttpRouter
import io.kairos.fault.Fault
import io.kairos.id.{PlanId, JobId}
import io.kairos.protocol.RegistryProtocol
import io.kairos.protocol.SchedulerProtocol
import io.kairos.time.TimeSource
import io.kairos.{JobSpec, Validated}
import org.slf4s.Logging

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by alonsodomin on 13/12/2015.
  */
class Kairos(settings: KairosClusterSettings)
            (implicit system: ActorSystem, materializer: ActorMaterializer, timeSource: TimeSource)
    extends HttpRouter with ServerFacade with Logging {

  import RegistryProtocol._
  import SchedulerProtocol._
  import UserAuthenticator._

  implicit val apiDispatcher = system.dispatchers.lookup("kairos.api-dispatcher")

  val core = system.actorOf(KairosCluster.props(settings), "kairos")

  val userAuth = system.actorSelection(core.path / "authenticator")

  def start(implicit timeout: Timeout): Future[Unit] = {
    Http().bindAndHandle(router, settings.httpInterface, settings.httpPort).
      map(_ => log.info(s"HTTP server started on ${settings.httpInterface}:${settings.httpPort}"))
  }

  def allExecutionPlanIds: Future[List[PlanId]] = {
    implicit val timeout = Timeout(5 seconds)
    (core ? GetExecutionPlans).mapTo[List[PlanId]]
  }

  def executionPlan(planId: PlanId): Future[Schedule] = {
    implicit val timeout = Timeout(5 seconds)

    (core ? GetExecutionPlan(planId)).map {
      case state: ExecutionPlan.PlanState =>
        Schedule(
          state.jobId, state.planId, state.trigger, state.lastExecutionTime
        )
    }
  }

  def schedule(schedule: ScheduleJob): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    implicit val timeout = Timeout(5 seconds)

    (core ? schedule) map {
      case invalid: JobNotFound        => Left(invalid)
      case valid: ExecutionPlanStarted => Right(valid)
    }
  }

  def fetchJob(jobId: JobId): Future[Option[JobSpec]] = {
    implicit val timeout = Timeout(5 seconds)
    (core ? GetJob(jobId)).mapTo[Option[JobSpec]]
  }

  def registerJob(jobSpec: JobSpec): Future[Validated[JobId]] = {
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

  def registeredJobs: Future[Map[JobId, JobSpec]] = {
    implicit val timeout = Timeout(5 seconds)
    (core ? GetJobs).mapTo[Map[JobId, JobSpec]]
  }

  def events = Source.actorPublisher[KairosClusterEvent](KairosEventEmitter.props).
    map(evt => {
      import upickle.default._
      ServerSentEvent(write[KairosClusterEvent](evt))
    })

  def clusterDetails: Future[ClusterInfo] = {
    implicit val timeout = Timeout(5 seconds)

    (core ? GetClusterStatus).mapTo[KairosStatus] map { status =>
      val nodeInfo = NodeInfo(status.members.size)
      ClusterInfo(nodeInfo, 0)
    }
  }

  def authenticate(username: String, password: Array[Char]): Future[Option[AuthInfo]] = {
    implicit val timeout = Timeout(5 seconds)

    userAuth ? Authenticate(username, password) map {
      case AuthenticationSuccess(authInfo) => Some(authInfo)
      case AuthenticationFailed => None
    }
  }

}
