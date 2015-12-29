package io.kairos.cluster

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import de.heikoseeberger.akkasse.ServerSentEvent
import io.kairos.JobSpec
import io.kairos.cluster.core._
import io.kairos.cluster.protocol.GetClusterStatus
import io.kairos.console.info.{ClusterInfo, NodeInfo}
import io.kairos.console.protocol.RegisterJobResponse
import io.kairos.console.server.ServerFacade
import io.kairos.console.server.http.HttpRouter
import io.kairos.console.server.security.AuthInfo
import io.kairos.id.JobId
import io.kairos.protocol.Error
import io.kairos.time.TimeSource
import org.slf4s.Logging

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by alonsodomin on 13/12/2015.
  */
object KairosCluster {

  final val DefaultSessionTimeout: FiniteDuration = 30 minutes

}

class KairosCluster(settings: KairosClusterSettings)
                   (implicit system: ActorSystem, materializer: ActorMaterializer, timeSource: TimeSource)
  extends HttpRouter with ServerFacade with Logging {

  import KairosCluster._
  import UserAuthenticator._
  import io.kairos.protocol.RegistryProtocol._

  val clusterSupervisor = system.actorOf(KairosClusterSupervisor.props(settings), "kairosSupervisor")
  val registry = system.actorSelection(clusterSupervisor.path / "registry")

  val userAuth = system.actorOf(UserAuthenticator.props(DefaultSessionTimeout), "authenticator")

  val journal = KairosJournal(system)

  def start(implicit timeout: Timeout): Future[Unit] = {
    import system.dispatcher

    Http().bindAndHandle(router, settings.httpInterface, settings.httpPort).
      map(_ => log.info(s"HTTP server started on ${settings.httpInterface}:${settings.httpPort}"))
  }

  def registerJob(jobSpec: JobSpec): Future[RegisterJobResponse] = {
    import system.dispatcher

    import scalaz._
    import Scalaz._

    implicit val timeout = Timeout(10 seconds)
    (registry ? RegisterJob(jobSpec)) map {
      case JobAccepted(jobId, _) => jobId.successNel[Error]
      case JobRejected(_, cause) => cause.failure[JobId]
    }
  }

  def registeredJobs: Future[Map[JobId, JobSpec]] =
    journal.registeredJobs

  def events = Source.actorPublisher[KairosClusterEvent](KairosEventEmitter.props).
    map(evt => {
      import upickle.default._
      ServerSentEvent(write[KairosClusterEvent](evt))
    })

  def clusterDetails: Future[ClusterInfo] = {
    import system.dispatcher
    implicit val timeout = Timeout(5 seconds)
    (clusterSupervisor ? GetClusterStatus).mapTo[KairosStatus] map { status =>
      val nodeInfo = NodeInfo(status.members.size)
      ClusterInfo(nodeInfo, 0)
    }
  }

  def authenticate(username: String, password: Array[Char]): Future[Option[AuthInfo]] = {
    import system.dispatcher

    implicit val timeout = Timeout(5 seconds)
    userAuth ? Authenticate(username, password) map {
      case AuthenticationSuccess(authInfo) => Some(authInfo)
      case AuthenticationFailed => None
    }
  }

}
