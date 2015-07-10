package io.chronos

import akka.actor.{Actor, ActorLogging, Props}
import akka.contrib.pattern.ClusterSingletonProxy
import akka.pattern._
import akka.util.Timeout
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
      role = Some("scheduler")
    ),
    name = "schedulerProxy"
  )

  def receive = {
    case p: PublishJob =>
      log.info("Publishing job spec. spec={}", p.job.id)
      schedulerProxy ! p

    case s: ScheduleJob =>
      log.info("Scheduling job. jobId={}", s.schedule.jobId)
      implicit val timeout = Timeout(5.seconds)
      (schedulerProxy ? s) map {
        case ScheduleAck(jobId) => JobAccepted
      } recover {
        case _ => JobRejected
      } pipeTo sender()

  }

}
