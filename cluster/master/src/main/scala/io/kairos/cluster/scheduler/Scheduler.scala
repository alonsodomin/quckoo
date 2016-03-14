package io.kairos.cluster.scheduler

import java.util.UUID

import akka.actor._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.pattern._
import akka.stream.{ActorMaterializerSettings, ActorMaterializer}
import io.kairos.cluster.core.KairosJournal
import io.kairos.cluster.protocol.WorkerProtocol
import io.kairos.id._
import io.kairos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.kairos.time.TimeSource
import io.kairos.{JobSpec, Task}

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Scheduler {
  import SchedulerProtocol._

  private[scheduler] case class CreateExecutionPlan(spec: JobSpec, config: ScheduleJob)

  def props(registry: ActorRef, queueProps: Props)(implicit timeSource: TimeSource) =
    Props(classOf[Scheduler], registry, queueProps, timeSource)

}

class Scheduler(registry: ActorRef, queueProps: Props)(implicit timeSource: TimeSource)
  extends Actor with ActorLogging with KairosJournal {

  import RegistryProtocol._
  import Scheduler._
  import SchedulerProtocol._
  import WorkerProtocol._

  ClusterClientReceptionist(context.system).registerService(self)

  final implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  private[this] val taskQueue = context.actorOf(queueProps, "taskQueue")
  private[this] val shardRegion = ClusterSharding(context.system).start(
    ExecutionPlan.ShardName,
    entityProps     = ExecutionPlan.props,
    settings        = ClusterShardingSettings(context.system),
    extractEntityId = ExecutionPlan.idExtractor,
    extractShardId  = ExecutionPlan.shardResolver
  )

  override implicit def actorSystem: ActorSystem = context.system

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      val handler = context.actorOf(handlerProps(cmd.jobId, sender(), cmd), "handler")
      registry.tell(GetJob(cmd.jobId), handler)

    case CreateExecutionPlan(spec, config) =>
      val planId = UUID.randomUUID()
      def executionProps(taskId: TaskId, jobSpec: JobSpec): Props = {
        val task = Task(taskId, jobSpec.artifactId, config.params, jobSpec.jobClass)
        Execution.props(planId, task, taskQueue, executionTimeout = config.timeout)
      }
      log.info("Starting execution plan for job {}.", config.jobId)
      shardRegion ! ExecutionPlan.New(config.jobId, spec, planId, config.trigger, executionProps)

    case get: GetExecutionPlan =>
      shardRegion.tell(get, sender())

    case GetExecutionPlans =>
      import context.dispatcher
      readJournal.eventsByTag(SchedulerTagEventAdapter.tags.ExecutionPlan, 0).
        filter(env =>
          env.event match {
            case evt: ExecutionPlan.Created => true
            case _                          => false
          }
        ).map(env =>
          env.event.asInstanceOf[ExecutionPlan.Created].cmd.planId
        ).runFold(List.empty[PlanId]) {
          case (list, planId) => planId :: list
        } pipeTo sender()

    case msg: WorkerMessage =>
      taskQueue.tell(msg, sender())
  }

  private[this] def handlerProps(jobId: JobId, requestor: ActorRef, config: ScheduleJob): Props =
    Props(classOf[ScheduleHandler], jobId, requestor, config)

}

private class ScheduleHandler(jobId: JobId, requestor: ActorRef, config: SchedulerProtocol.ScheduleJob)
    extends Actor with ActorLogging {

  import Scheduler._
  import SchedulerProtocol._

  def receive: Receive = {
    case Some(spec: JobSpec) => // create execution plan
      context.parent ! CreateExecutionPlan(spec, config)
      context.stop(self)

    case None =>
      log.warning("No job with id {} could be retrieved.", jobId)
      requestor ! JobNotFound(jobId)
      context.stop(self)
  }

}