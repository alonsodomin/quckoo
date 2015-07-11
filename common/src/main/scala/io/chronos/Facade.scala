package io.chronos

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.contrib.pattern._
import akka.pattern._
import akka.util.Timeout
import io.chronos.protocol.SchedulerProtocol

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

object Facade {

  def props(client: ActorRef): Props = Props(classOf[Facade], client)
  
  trait Unreachable
  
  sealed trait FacadeResponse
  case object JobAccepted extends FacadeResponse
  case object JobRejected extends FacadeResponse with Unreachable

  case object ResourceUnreachable extends Unreachable
}

class Facade(repositoryClient: ActorRef) extends Actor with ActorLogging {
  import ClusterClient._
  import Facade._
  import SchedulerProtocol._
  import context.dispatcher
  
  def receive = {
    case p: PublishJob =>
      log.info("Publishing job spec. spec={}", p.job.id)
      repositoryClient ! Send(path.Repository, p, localAffinity = false)

    case s: ScheduleJob =>
      log.info("Scheduling job. jobId={}", s.schedule.jobId)
      implicit val timeout = Timeout(5.seconds)
      (repositoryClient ? SendToAll(path.Scheduler, s)) map {
        case ScheduleAck(jobId) => JobAccepted
      } recover {
        case _ => JobRejected
      } pipeTo sender()

  }

}
