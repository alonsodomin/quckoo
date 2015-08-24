package io.chronos.scheduler

import java.time.Clock
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import io.chronos.JobSpec
import io.chronos.cluster._
import io.chronos.cluster.protocol.WorkerProtocol
import io.chronos.id._
import io.chronos.protocol.RegistryProtocol.JobNotEnabled
import io.chronos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.chronos.scheduler.execution.{Execution, ExecutionPlan}
import io.chronos.scheduler.queue.TaskQueue

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Scheduler {

  def props(shardSettings: ClusterShardingSettings, registry: ActorRef, queueProps: Props)(implicit clock: Clock) =
    Props(classOf[Scheduler], shardSettings, registry, queueProps, clock)

  private object WorkerState {
    sealed trait WorkerStatus

    case object Idle extends WorkerStatus
    case class Busy(taskId: TaskId, deadline: Deadline) extends WorkerStatus
  }

  private case class WorkerState(ref: ActorRef, status: WorkerState.WorkerStatus)

  private case object CleanupTick

}

class Scheduler(shardSettings: ClusterShardingSettings, registry: ActorRef, queueProps: Props)(implicit clock: Clock)
  extends Actor with ActorLogging {

  import DistributedPubSubMediator._
  import RegistryProtocol._
  import Scheduler._
  import SchedulerProtocol._
  import WorkerProtocol._

  ClusterClientReceptionist(context.system).registerService(self)

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(TaskQueue.shardName, self)

  private val taskQueue = ClusterSharding(context.system).start(
    typeName        = TaskQueue.shardName,
    entityProps     = queueProps,
    settings        = shardSettings,
    extractEntityId = TaskQueue.idExtractor,
    extractShardId  = TaskQueue.shardResolver
  )

  private var workers = Map.empty[WorkerId, WorkerState]

  override def receive: Receive = {
    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        workers += (workerId -> WorkerState(sender(), status = WorkerState.Idle))
        log.info("Worker registered. workerId={}, location={}", workerId, sender().path)
      }

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

private class ScheduleHandler(jobId: JobId, requestor: ActorRef, executionPlan: () => ActorRef)
  extends Actor with ActorLogging {

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