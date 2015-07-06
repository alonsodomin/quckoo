package io.chronos.scheduler.servant

import akka.actor.Actor
import akka.contrib.pattern.ClusterSingletonProxy
import akka.pattern._
import akka.util.Timeout
import io.chronos.scheduler.butler.Butler

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

  var butlerProxy = context.actorOf(ClusterSingletonProxy.props(
    singletonPath = Butler.Path,
    role = Some("backend")
  ), name = "butlerProxy")

  def receive = {
    case work =>
      implicit val timeout = Timeout(5.seconds)
      (butlerProxy ? work) map {
        case Butler.Ack(_) => Accepted
      } recover {
        case _ => Rejected
      } pipeTo sender()
  }

}
