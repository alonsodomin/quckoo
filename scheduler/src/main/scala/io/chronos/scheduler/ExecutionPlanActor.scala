package io.chronos.scheduler

import java.time.Clock

import akka.actor.{Actor, ActorLogging, Props}
import akka.contrib.pattern._
import io.chronos.protocol._
import io.chronos.scheduler.internal.cache.{ExecutionCache, ScheduleCache}
import io.chronos.topic

/**
 * Created by aalonsodominguez on 03/08/15.
 */
object ExecutionPlanActor {

  def props(scheduleCache: ScheduleCache, executionCache: ExecutionCache)(implicit clock: Clock): Props =
    Props(classOf[ExecutionPlanActor], scheduleCache, executionCache, clock)

}

class ExecutionPlanActor(scheduleCache: ScheduleCache, executionCache: ExecutionCache)(implicit clock: Clock) extends Actor with ActorLogging {

  ClusterReceptionistExtension(context.system).registerService(self)
  private val mediator = DistributedPubSubExtension(context.system).mediator

  def receive = {
    case ScheduleJob(schedule) =>
      val scheduleId = scheduleCache += schedule
      val execution = executionCache += scheduleId
      mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(execution.executionId, execution.stage))
      sender ! ScheduleJobAck(execution.executionId)

    case RescheduleJob(scheduleId) =>
      val execution = executionCache += scheduleId
      mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(execution.executionId, execution.stage))
      sender ! ScheduleJobAck(execution.executionId)

    case GetSchedule(scheduleId) =>
      sender ! scheduleCache(scheduleId)

    case GetScheduledJobs =>
      sender ! scheduleCache.toTraversable

    case GetExecution(executionId) =>
      sender ! executionCache(executionId)

    case req: GetExecutions =>
      sender ! executionCache.toTraversable.filter(entry => req.filter(entry._2))
  }

}
