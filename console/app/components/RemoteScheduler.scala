package components

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, AddressFromURIString, RootActorPath}
import akka.contrib.pattern.ClusterClient
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.japi.Util._
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.chronos.id.ScheduleId
import io.chronos.protocol.SchedulerProtocol
import io.chronos.{JobSchedule, JobSpec, path}

import scala.concurrent._
import scala.concurrent.duration._

/**
 * Created by domingueza on 09/07/15.
 */
@Singleton
class RemoteScheduler @Inject() (system: ActorSystem) {
  import SchedulerProtocol._

  val chronosConf = ConfigFactory.load("chronos")

  val initialContacts = immutableSeq(chronosConf.getStringList("chronos.seed-nodes")).map {
    case AddressFromURIString(addr) => system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
  }.toSet

  val chronosClient = system.actorOf(ClusterClient.props(initialContacts), "chronosClient")

  def jobDefinitions: Future[Seq[JobSpec]] = {
    implicit val xc: ExecutionContext = ExecutionContext.global
    implicit val timeout = Timeout(10.seconds)

    (chronosClient ? SendToAll(path.Scheduler, GetJobSpecs)).asInstanceOf[Future[JobSpecs]] map { response => response.specs }
  }

  def scheduledJobs: Future[Seq[(ScheduleId, JobSchedule)]] = {
    implicit val xc: ExecutionContext = ExecutionContext.global
    implicit val timeout = Timeout(10.seconds)

    (chronosClient ? SendToAll(path.Scheduler, GetScheduledJobs)).asInstanceOf[Future[ScheduledJobs]] map { response => response.jobs }
  }

}
