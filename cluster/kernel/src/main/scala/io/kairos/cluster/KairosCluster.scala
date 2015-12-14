package io.kairos.cluster

import java.time.Clock

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import de.heikoseeberger.akkasse.ServerSentEvent
import io.kairos.cluster.core.{KairosClusterEvent, KairosEventEmitter, KairosClusterSupervisor, UserAuthenticator}
import io.kairos.cluster.protocol.GetClusterStatus
import io.kairos.console.protocol.ClusterDetails
import io.kairos.console.server.ServerFacade
import io.kairos.console.server.http.HttpRouter
import io.kairos.console.server.security.AuthInfo
import org.slf4s.Logging

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by alonsodomin on 13/12/2015.
  */
object KairosCluster {

  final val DefaultSessionTimeout: FiniteDuration = 30 minutes

}

class KairosCluster(settings: KairosClusterSettings)(implicit system: ActorSystem, materializer: ActorMaterializer, clock: Clock)
  extends HttpRouter with ServerFacade with Logging {

  import KairosCluster._
  import UserAuthenticator._

  val clusterSupervisor = system.actorOf(KairosClusterSupervisor.props(settings), "kairosSupervisor")

  val userAuth = system.actorOf(UserAuthenticator.props(DefaultSessionTimeout), "authenticator")

  def start(implicit timeout: Timeout): Future[Unit] = {
    import system.dispatcher

    Http().bindAndHandle(router, settings.httpInterface, settings.httpPort).
      map(_ => log.info(s"HTTP server started on ${settings.httpInterface}:${settings.httpPort}"))
  }

  def events = Source.actorPublisher[KairosClusterEvent](KairosEventEmitter.props).
    map(evt => {
      import upickle.default._
      ServerSentEvent(write[KairosClusterEvent](evt))
    })

  def clusterDetails: Future[ClusterDetails] = {
    import system.dispatcher
    implicit val timeout = Timeout(5 seconds)
    (clusterSupervisor ? GetClusterStatus).mapTo[KairosStatus] map { status =>
      ClusterDetails(status.members.size, 0)
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
