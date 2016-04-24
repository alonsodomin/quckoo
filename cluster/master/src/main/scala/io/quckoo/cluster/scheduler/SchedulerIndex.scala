package io.quckoo.cluster.scheduler

import akka.actor.Actor

import io.quckoo.id.PlanId

object SchedulerIndex {

  final case class IndexExecutionPlan(plan: PlanId)

}

class SchedulerIndex extends Actor {
  import SchedulerIndex._

  def receive: Receive = {
    case IndexExecutionPlan(planId) =>
    
  }

}
