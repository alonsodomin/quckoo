package io.chronos.scheduler

import java.time.Clock

import akka.actor.{Actor, ActorLogging, Props}
import akka.contrib.pattern._
import io.chronos.protocol._
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

    case RescheduleJob(scheduleId) =>
      val execution = executionPlan.reschedule(scheduleId)
      mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(execution.executionId, execution.stage))
      sender ! ScheduleJobAck(execution.executionId)

    case GetSchedule(scheduleId) =>
      sender ! executionPlan.getSchedule(scheduleId)

    case GetScheduledJobs =>
      sender ! executionPlan.getScheduledJobs

    case GetExecution(executionId) =>
      sender ! executionPlan.getExecution(executionId)

    case req: GetExecutions =>
      sender ! executionPlan.getExecutions(req.filter)
  }

}
