package io.chronos.scheduler

import java.time.Clock
import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.cluster.Task
import io.chronos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.chronos.scheduler.execution.{Execution, ExecutionPlan}

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Scheduler {

  def props(registryProps: Props, queueProps: Props)(implicit clock: Clock) =
    Props(classOf[Scheduler], registryProps, queueProps, clock)

}

class Scheduler(registryProps: Props, queueProps: Props)(implicit clock: Clock) extends Actor with ActorLogging {
  import SchedulerProtocol._

  private val jobRegistry = context.actorOf(registryProps, "registry")
  private val taskQueue = context.actorOf(queueProps, "taskQueue")

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      val plan = context.actorOf(ExecutionPlan.props(cmd.trigger) { (planId, jobSpec) =>
        val task = Task(UUID.randomUUID(), jobSpec.moduleId, cmd.params, jobSpec.jobClass)
        Execution.props(planId, task, taskQueue, cmd.timeout)
      })
      jobRegistry.tell(RegistryProtocol.GetJob(cmd.jobId), plan)
  }

}
