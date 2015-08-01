package components

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.contrib.pattern.ClusterClient.{Send, SendToAll}
import akka.pattern._
import akka.util.Timeout
import io.chronos.id._
import io.chronos.protocol._
import io.chronos.{Execution, JobSchedule, JobSpec, path}

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 11/07/15.
 */
@Singleton
class ChronosClient @Inject() (system: ActorSystem) {

  private val chronosClient = system.actorSelection(system / "chronosClient")

  def availableJobSpecs: Future[Seq[JobSpec]] = {
    implicit val timeout = Timeout(10.seconds)

    (chronosClient ? Send(path.Registry, GetJobs, localAffinity = false)).
      mapTo[Seq[JobSpec]]
  }

  def scheduledJobs: Future[Seq[(ScheduleId, JobSchedule)]] = {
    implicit val timeout = Timeout(10.seconds)

    (chronosClient ? SendToAll(path.Scheduler, GetScheduledJobs)).
      mapTo[Seq[(ScheduleId, JobSchedule)]]
  }

  def executions: Future[Seq[Execution]] = {
    implicit val timeout = Timeout(10.seconds)

    (chronosClient ? SendToAll(path.Scheduler, GetExecutions)).
      mapTo[Seq[Execution]]
  }

}
