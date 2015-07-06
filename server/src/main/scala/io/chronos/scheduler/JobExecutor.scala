package io.chronos.scheduler

import akka.actor.Actor

/**
 * Created by aalonsodominguez on 05/07/15.
 */
class JobExecutor extends Actor {

  def receive = {
    case n: Int =>
      val n2 = n * n
      val result = s"$n * $n = $n2"
      sender() ! Worker.WorkComplete(result)
  }

}
