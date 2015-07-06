package io.chronos.scheduler.receptor

import java.util.UUID

import akka.actor.Actor
import akka.contrib.pattern.ClusterSingletonProxy
import akka.pattern._
import akka.util.Timeout
import io.chronos.scheduler.JobDefinition
import io.chronos.scheduler.butler.Butler
import io.chronos.scheduler.worker.Work

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

object ReceptorActor {
  sealed trait ReceptorMessage
  case class Register(job: JobDefinition) extends ReceptorMessage

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
    case Register(job) =>
      val workId = nextWorkId
      val work = Work(workId, job.execution)
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
