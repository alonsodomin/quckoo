package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging}
import io.chronos.scheduler.internal.{DistributedExecutionCache, DistributedScheduleCache}
import org.apache.ignite.Ignite

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object ExecutionPlanActor {

  private case object Heartbeat

}

class ExecutionPlanActor(val ignite: Ignite, heartbeatInterval: FiniteDuration)
  extends Actor with ActorLogging with DistributedScheduleCache with DistributedExecutionCache {

  import ExecutionPlanActor._

  def receive = {
    case Heartbeat =>
  }

}
