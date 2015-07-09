package components

import javax.inject.{Inject, Singleton}

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import com.google.inject.name.Named
import io.chronos.JobDefinition
import io.chronos.protocol.SchedulerProtocol

import scala.concurrent._
import scala.concurrent.duration._

/**
 * Created by domingueza on 09/07/15.
 */
@Singleton
class RemoteScheduler @Inject() (@Named("chronos") chronosClient: ActorRef) {

  def jobDefinitions: Future[Seq[JobDefinition]] = {
    implicit val xc: ExecutionContext = ExecutionContext.global
    implicit val timeout = Timeout(5.seconds)
    (chronosClient ? SchedulerProtocol.GetScheduledJobs).
      asInstanceOf[Future[SchedulerProtocol.ScheduledJobs]] map { response => response.jobs }
  }

}
