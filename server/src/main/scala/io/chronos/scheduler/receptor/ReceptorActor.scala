package io.chronos.scheduler.receptor

import java.util.UUID

import akka.actor.Actor
import akka.contrib.pattern.ClusterSingletonProxy
import akka.pattern._
import akka.util.Timeout
import io.chronos.scheduler.Job
import io.chronos.scheduler.butler.Butler
import io.chronos.scheduler.worker.Work

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

object ReceptorActor {
  sealed trait ReceptorMessage
  case class EnqueueJob(job: Any) extends ReceptorMessage

  sealed trait ReceptorResponse
  case object Accepted extends ReceptorResponse
  case object Rejected extends ReceptorResponse
}

class ReceptorActor extends Actor {
  import ReceptorActor._
  import context.dispatcher

  var butlerProxy = context.actorOf(ClusterSingletonProxy.props(
    singletonPath = Butler.Path,
    role = Some("backend")
  ), name = "butlerProxy")

  def nextWorkId = UUID.randomUUID().toString

  def receive = {
    case EnqueueJob(job: Job.Execution) =>
      val work = Work(nextWorkId, job)
      implicit val timeout = Timeout(5.seconds)
      (butlerProxy ? work) map {
        case Butler.Ack(_) => Accepted
      } recover {
        case _ => Rejected
      } pipeTo sender()

    case work =>
      implicit val timeout = Timeout(5.seconds)
      (butlerProxy ? work) map {
        case Butler.Ack(_) => Accepted
      } recover {
        case _ => Rejected
      } pipeTo sender()
  }

}
