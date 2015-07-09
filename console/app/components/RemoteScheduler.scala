package components

import javax.inject.{Inject, Singleton}

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import io.chronos.JobDefinition
import io.chronos.protocol.SchedulerProtocol

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by domingueza on 09/07/15.
 */
@Singleton
class RemoteScheduler @Inject() (val chronosFacade: ActorRef) {

  def jobDefinitions: Future[Seq[JobDefinition]] = {
    implicit val timeout = Timeout(5.seconds)
    (chronosFacade ? SchedulerProtocol.GetScheduledJobs).
      asInstanceOf[Future[SchedulerProtocol.ScheduledJobs]] map { response => response.jobs }
  }

}
