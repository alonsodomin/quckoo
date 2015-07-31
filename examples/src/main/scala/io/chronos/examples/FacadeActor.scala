package io.chronos.examples

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.contrib.pattern._
import akka.pattern._
import akka.util.Timeout
import io.chronos.path
import io.chronos.protocol._

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

object FacadeActor {

  def props(client: ActorRef): Props = Props(classOf[FacadeActor], client)
  
}

class FacadeActor(client: ActorRef) extends Actor with ActorLogging {
  import ClusterClient._
  import context.dispatcher

  implicit val timeout = Timeout(10 seconds)

  def receive = {
    case p: RegisterJob =>
      log.info("Publishing job spec. spec={}", p.job.id)
      (client ? Send(path.Registry, p, localAffinity = false)) pipeTo sender()

    case s: ScheduleJob =>
      log.info("Scheduling job. jobId={}, desc={}", s.schedule.jobId, s.schedule)
      (client ? SendToAll(path.Scheduler, s)) recover {
        case e: Throwable => ScheduleJobFailed(Right(e))
      } pipeTo sender()

  }

}
