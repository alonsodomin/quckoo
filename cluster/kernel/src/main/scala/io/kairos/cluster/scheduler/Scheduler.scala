package io.kairos.cluster.scheduler

import java.util.UUID

import akka.actor._
import akka.cluster.client.ClusterClientReceptionist
import io.kairos.JobSpec
import io.kairos.cluster._
import io.kairos.cluster.protocol.WorkerProtocol
import io.kairos.cluster.scheduler.execution.{ExecutionFSM, ExecutionPlan}
import io.kairos.id._
import io.kairos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.kairos.time.TimeSource

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Scheduler {

  def props(registry: ActorRef, queueProps: Props)(implicit timeSource: TimeSource) =
    Props(classOf[Scheduler], registry, queueProps, timeSource)

}

class Scheduler(registry: ActorRef, queueProps: Props)(implicit timeSource: TimeSource)
  extends Actor with ActorLogging {

  import RegistryProtocol._
  import SchedulerProtocol._
  import WorkerProtocol._

  ClusterClientReceptionist(context.system).registerService(self)

  private[this] val taskQueue = context.actorOf(queueProps, "taskQueue")

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      def executionPlanProps(planId: PlanId): Props = ExecutionPlan.props(planId, cmd.trigger) { (taskId, jobSpec) =>
        val task = Task(taskId, jobSpec.moduleId, cmd.params, jobSpec.jobClass)
        ExecutionFSM.props(planId, task, taskQueue, executionTimeout = cmd.timeout)
      }
      val handler = context.actorOf(handlerProps(cmd.jobId, sender()) { () =>
        val planId = UUID.randomUUID()
        context.actorOf(executionPlanProps(planId), "plan-" + planId)
      })
      registry.tell(GetJob(cmd.jobId), handler)

    case msg: WorkerMessage =>
      taskQueue.tell(msg, sender())
  }

  private[this] def handlerProps(jobId: JobId, requestor: ActorRef)(executionPlan: () => ActorRef): Props =
    Props(classOf[ScheduleHandler], jobId, requestor, executionPlan)

}

private class ScheduleHandler(jobId: JobId, requestor: ActorRef, executionPlan: () => ActorRef)
  extends Actor with ActorLogging {

  import RegistryProtocol._

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