package io.chronos.facade

import akka.actor.{Actor, ActorLogging, Props}
import akka.contrib.pattern.ClusterSingletonProxy
import akka.pattern._
import akka.util.Timeout
import io.chronos.path
import io.chronos.protocol.SchedulerProtocol

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

object Facade {

  def props(): Props = Props(classOf[Facade])
  
  trait Unreachable
  
  sealed trait FacadeResponse
  case object JobAccepted extends FacadeResponse
  case object JobRejected extends FacadeResponse with Unreachable

  case object ResourceUnreachable extends Unreachable
}

class Facade extends Actor with ActorLogging {
  import Facade._
  import SchedulerProtocol._
  import context.dispatcher

  var schedulerProxy = context.actorOf(
    ClusterSingletonProxy.props(
      singletonPath = path.Scheduler,
      role = Some("backend")
    ),
    name = "schedulerProxy"
  )

  def receive = {
    case s: ScheduleJob =>
      log.info("Registering job. jobId={}", s.jobDefinition.jobId)
      implicit val timeout = Timeout(5.seconds)
      (schedulerProxy ? s) map {
        case ScheduleAck(jobId) => JobAccepted
      } recover {
        case _ => JobRejected
      } pipeTo sender()

    case msg: GetScheduledJobs =>
      implicit val timeout = Timeout(5.seconds)
      (schedulerProxy ? msg) map {
        case res: ScheduledJobs => res
      } recover {
        case _ => ResourceUnreachable
      } pipeTo sender()
  }

}
