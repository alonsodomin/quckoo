package io.chronos.scheduler

import java.time.Clock
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import io.chronos.JobSpec
import io.chronos.cluster.Task
import io.chronos.id.{JobId, PlanId}
import io.chronos.protocol.RegistryProtocol.JobNotEnabled
import io.chronos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.chronos.scheduler.execution.{Execution, ExecutionPlan}

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Scheduler {

  def props(shardSettings: ClusterShardingSettings, registryProps: Props, queueProps: Props)(implicit clock: Clock) =
    Props(classOf[Scheduler], shardSettings, registryProps, queueProps, clock)

}

class Scheduler(shardSettings: ClusterShardingSettings, registryProps: Props, queueProps: Props)(implicit clock: Clock)
  extends Actor with ActorLogging {

  import RegistryProtocol._
  import SchedulerProtocol._

  ClusterClientReceptionist(context.system).registerService(self)

  private val registry = ClusterSharding(context.system).start(
    typeName = Registry.shardName,
    entityProps = registryProps,
    settings = shardSettings,
    extractEntityId = Registry.idExtractor,
    extractShardId = Registry.shardResolver
  )

  private val taskQueue = context.actorOf(queueProps, "taskQueue")

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      def executionPlanProps(planId: PlanId): Props = ExecutionPlan.props(planId, cmd.trigger) { (taskId, jobSpec) =>
        val task = Task(taskId, jobSpec.moduleId, cmd.params, jobSpec.jobClass)
        Execution.props(planId, task, taskQueue, executionTimeout = cmd.timeout)
      }
      val handler = context.actorOf(handlerProps(cmd.jobId, sender()) { () =>
        val planId = UUID.randomUUID()
        context.actorOf(executionPlanProps(planId), "plan-" + planId)
      })
      registry.tell(GetJob(cmd.jobId), handler)
  }

  private def handlerProps(jobId: JobId, requestor: ActorRef)(executionPlan: () => ActorRef): Props =
    Props(classOf[ScheduleHandler], jobId, requestor, executionPlan)

}

private class ScheduleHandler(jobId: JobId, requestor: ActorRef, executionPlan: () => ActorRef) extends Actor with ActorLogging {

  def receive: Receive = {
    case spec: JobSpec => // create execution plan
      log.info("Scheduling job {}.", jobId)
      executionPlan().tell(jobId -> spec, requestor)
      context.stop(self)

    case jne: JobNotEnabled =>
      requestor ! jne
      context.stop(self)
  }

}