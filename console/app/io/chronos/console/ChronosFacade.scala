package io.chronos.console

import akka.actor.{Actor, ActorLogging, Props}

/**
 * Created by domingueza on 09/07/15.
 */

object ChronosFacade {

  def props: Props = Props(classOf[ChronosFacade])

  sealed trait ChronosRequest
  case object GetScheduledJobs extends ChronosRequest
}

class ChronosFacade extends Actor with ActorLogging {
  override def receive: Receive = ???
}
