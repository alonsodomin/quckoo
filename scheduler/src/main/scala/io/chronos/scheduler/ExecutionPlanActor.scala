package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging}

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object ExecutionPlanActor {

  private case object Heartbeat

}

class ExecutionPlanActor(executionPlan: ExecutionPlan,
                         executionQueue: ExecutionQueue,
                         heartbeatInterval: FiniteDuration)
  extends Actor with ActorLogging {

  import ExecutionPlanActor._

  def receive = {
    case Heartbeat =>
  }

}
