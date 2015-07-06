package io.chronos.scheduler

import akka.actor.Actor
import akka.contrib.pattern.ClusterSingletonProxy
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

object Frontend {
  case object Accepted
  case object Rejected
}

class Frontend extends Actor {
  import Frontend._
  import context.dispatcher

  var masterProxy = context.actorOf(ClusterSingletonProxy.props(
    singletonPath = "/user/master/active",
    role = Some("backend")
  ), name = "masterProxy")

  def receive = {
    case work =>
      implicit val timeout = Timeout(5.seconds)
      (masterProxy ? work) map {
        case Master.Ack(_) => Accepted
      } recover {
        case _ => Rejected
      } pipeTo sender()
  }

}
