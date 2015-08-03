package io.chronos.scheduler

import java.time.Clock

import akka.actor.{Actor, ActorLogging, Props}
import akka.contrib.pattern.{ClusterReceptionistExtension, DistributedPubSubExtension, DistributedPubSubMediator}
import io.chronos.protocol.{ExecutionEvent, ScheduleJob, ScheduleJobAck}
import io.chronos.topic

/**
 * Created by aalonsodominguez on 03/08/15.
 */
object ExecutionPlanActor {

  def props(executionPlan: ExecutionPlan)(implicit clock: Clock): Props =
    Props(classOf[ExecutionPlanActor], executionPlan, clock)

}

class ExecutionPlanActor(executionPlan: ExecutionPlan)(implicit clock: Clock) extends Actor with ActorLogging {

  ClusterReceptionistExtension(context.system).registerService(self)
  private val mediator = DistributedPubSubExtension(context.system).mediator

  def receive = {
    case ScheduleJob(schedule) =>
      val execution = executionPlan.schedule(schedule)
      mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(execution.executionId, execution.stage))
      sender ! ScheduleJobAck(execution.executionId)
  }

}
