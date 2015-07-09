package components

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, AddressFromURIString, RootActorPath}
import akka.contrib.pattern.ClusterClient
import akka.japi.Util._
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.chronos.JobDefinition
import io.chronos.protocol.SchedulerProtocol

import scala.concurrent._
import scala.concurrent.duration._

/**
 * Created by domingueza on 09/07/15.
 */
@Singleton
class RemoteScheduler @Inject() (system: ActorSystem) {

  val chronosConf = ConfigFactory.load("chronos")

  val initialContacts = immutableSeq(chronosConf.getStringList("chronos.seed-nodes")).map {
    case AddressFromURIString(addr) => system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
  }.toSet

  val chronosClient = system.actorOf(ClusterClient.props(initialContacts), "chronosClient")

  def jobDefinitions: Future[Seq[JobDefinition]] = {
    implicit val xc: ExecutionContext = ExecutionContext.global
    implicit val timeout = Timeout(5.seconds)
    (chronosClient ? SchedulerProtocol.GetScheduledJobs).
      asInstanceOf[Future[SchedulerProtocol.ScheduledJobs]] map { response => response.jobs }
  }

}
