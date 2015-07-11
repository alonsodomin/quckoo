package components

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, AddressFromURIString, RootActorPath}
import akka.contrib.pattern.ClusterClient
import akka.contrib.pattern.ClusterClient.{Send, SendToAll}
import akka.japi.Util._
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.chronos.id._
import io.chronos.protocol.SchedulerProtocol.{GetJobSpecs, GetScheduledJobs, JobSpecs, ScheduledJobs}
import io.chronos.{JobSchedule, JobSpec, path}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by aalonsodominguez on 11/07/15.
 */
@Singleton
class ChronosClient @Inject() (system: ActorSystem) {

  private val chronosConf = ConfigFactory.load("chronos")

  private val initialContacts = immutableSeq(chronosConf.getStringList("chronos.seed-nodes")).map {
    case AddressFromURIString(addr) => system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
  }.toSet

  private val chronosClient = system.actorOf(ClusterClient.props(initialContacts), "chronosClient")

  def availableJobSpecs: Future[Seq[JobSpec]] = {
    implicit val xc: ExecutionContext = ExecutionContext.global
    implicit val timeout = Timeout(10.seconds)

    (chronosClient ? Send(path.Repository, GetJobSpecs, localAffinity = false)).
      asInstanceOf[Future[JobSpecs]] map { response => response.specs }
  }

  def scheduledJobs: Future[Seq[(ScheduleId, JobSchedule)]] = {
    implicit val xc: ExecutionContext = ExecutionContext.global
    implicit val timeout = Timeout(10.seconds)

    (chronosClient ? SendToAll(path.Scheduler, GetScheduledJobs)).
      asInstanceOf[Future[ScheduledJobs]] map { response => response.jobs }
  }

}
