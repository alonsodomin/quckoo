package io.chronos.examples

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.contrib.pattern._
import akka.pattern._
import akka.util.Timeout
import io.chronos.path
import io.chronos.protocol.SchedulerProtocol

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

object FacadeActor {

  def props(client: ActorRef): Props = Props(classOf[FacadeActor], client)
  
  trait Unreachable
  
  sealed trait FacadeResponse
  case object JobAccepted extends FacadeResponse
  case object JobRejected extends FacadeResponse with Unreachable

  case object ResourceUnreachable extends Unreachable
}

class FacadeActor(client: ActorRef) extends Actor with ActorLogging {
  import ClusterClient._
  import FacadeActor._
  import SchedulerProtocol._
  import context.dispatcher
  
  def receive = {
    case p: RegisterJob =>
      log.info("Publishing job spec. spec={}", p.job.id)
      client ! Send(path.Repository, p, localAffinity = false)

    case s: ScheduleJob =>
      log.info("Scheduling job. jobId={}, desc={}", s.schedule.jobId, s.schedule)
      implicit val timeout = Timeout(5.seconds)
      (client ? SendToAll(path.Scheduler, s)) map {
        case ScheduleAck(jobId) => JobAccepted
      } recover {
        case _ => JobRejected
      } pipeTo sender()

  }

}
