package io.chronos.receptor

import akka.actor.{Actor, ActorLogging}
import akka.contrib.pattern.ClusterSingletonProxy
import akka.pattern._
import akka.util.Timeout
import io.chronos.scheduler.Scheduler
import io.chronos.scheduler.Scheduler.{ScheduleAck, ScheduleJob}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

object ReceptorActor {

  sealed trait ReceptorResponse
  case object Accepted extends ReceptorResponse
  case object Rejected extends ReceptorResponse
}

class ReceptorActor extends Actor with ActorLogging {
  import ReceptorActor._
  import context.dispatcher

  var schedulerProxy = context.actorOf(
    ClusterSingletonProxy.props(
      singletonPath = Scheduler.Path,
      role = Some("backend")
    ),
    name = "schedulerProxy"
  )

  def receive = {
    case s: ScheduleJob =>
      log.info("Registering job. jobId={}", s.jobDefinition.jobId)
      implicit val timeout = Timeout(5.seconds)
      (schedulerProxy ? s) map {
        case ScheduleAck(jobId) => Accepted
      } recover {
        case _ => Rejected
      } pipeTo sender()
  }

}
