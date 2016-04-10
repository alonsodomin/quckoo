package io.quckoo.cluster.scheduler

import java.util.UUID

import akka.actor._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.persistence.query.scaladsl.{AllPersistenceIdsQuery, EventsByPersistenceIdQuery}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import io.quckoo.JobSpec
import io.quckoo.cluster.protocol._
import io.quckoo.cluster.topics
import io.quckoo.id._
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.time.TimeSource

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Scheduler {

  type Journal = AllPersistenceIdsQuery with EventsByPersistenceIdQuery

  private[scheduler] case class CreateExecutionDriver(spec: JobSpec, config: ScheduleJob, requestor: ActorRef)

  def props(registry: ActorRef, readJournal: Scheduler.Journal, queueProps: Props)(implicit timeSource: TimeSource) =
    Props(classOf[Scheduler], registry, readJournal, queueProps, timeSource)

}

class Scheduler(registry: ActorRef, readJournal: Scheduler.Journal, queueProps: Props)(implicit timeSource: TimeSource)
    extends Actor with ActorLogging {

  import Scheduler._

  ClusterClientReceptionist(context.system).registerService(self)

  final implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  private[this] val taskQueue = context.actorOf(queueProps, "queue")
  private[this] val shardRegion = ClusterSharding(context.system).start(
    ExecutionDriver.ShardName,
    entityProps     = ExecutionDriver.props,
    settings        = ClusterShardingSettings(context.system),
    extractEntityId = ExecutionDriver.idExtractor,
    extractShardId  = ExecutionDriver.shardResolver
  )
  private[this] val executionPlanView = context.actorOf(
    Props(classOf[ExecutionPlanView], shardRegion), "plans"
  )

  override def preStart(): Unit = {
    readJournal.allPersistenceIds().
      filter(_.startsWith("ExecutionPlan-")).
      flatMapConcat { persistenceId =>
        readJournal.eventsByPersistenceId(persistenceId, 0, System.currentTimeMillis())
      } runForeach { env =>
        executionPlanView ! env.event
      }
  }

  override def postStop(): Unit =
    context.stop(executionPlanView)

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      val handler = context.actorOf(jobFetcherProps(cmd.jobId, sender(), cmd), "handler")
      registry.tell(GetJob(cmd.jobId), handler)

    case cmd @ CreateExecutionDriver(_, config, _) =>
      val planId = UUID.randomUUID()
      val props = factoryProps(config.jobId, planId, cmd, shardRegion)
      log.debug("Found enabled job {}. Initializing a new execution plan for it.", config.jobId)
      context.actorOf(props, s"execution-driver-factory-$planId")

    case get: GetExecutionPlan =>
      executionPlanView.tell(get, sender())

    case GetExecutionPlans =>
      executionPlanView.tell(GetExecutionPlans, sender())

    case msg: WorkerMessage =>
      taskQueue.tell(msg, sender())
  }

  private[this] def jobFetcherProps(jobId: JobId, requestor: ActorRef, config: ScheduleJob): Props =
    Props(classOf[JobFetcher], jobId, requestor, config)

  private[this] def factoryProps(jobId: JobId, planId: PlanId, createCmd: CreateExecutionDriver,
                                 shardRegion: ActorRef): Props =
    Props(classOf[ExecutionDriverFactory], jobId, planId, createCmd, shardRegion)

}

private class JobFetcher(jobId: JobId, requestor: ActorRef, config: ScheduleJob)
    extends Actor with ActorLogging {

  import Scheduler._

  def receive: Receive = {
    case Some(spec: JobSpec) =>
      if (!spec.disabled) {
        // create execution plan
        context.parent ! CreateExecutionDriver(spec, config, requestor)
      } else {
        log.info("Found job {} in the registry but is not enabled.", jobId)
        requestor ! JobNotEnabled(jobId)
      }
      context.stop(self)

    case None =>
      log.info("No enabled job with id {} could be retrieved.", jobId)
      requestor ! JobNotFound(jobId)
      context.stop(self)
  }

}

private class ExecutionDriverFactory(
    jobId: JobId,
    planId: PlanId,
    cmd: Scheduler.CreateExecutionDriver,
    shardRegion: ActorRef)
  extends Actor with ActorLogging {

  private[this] val mediator = DistributedPubSub(context.system).mediator

  override def preStart(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(topics.Scheduler, self)

  def receive: Receive = initializing

  def initializing: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      import cmd._

      log.debug("Starting execution plan for job {}.", jobId)
      val executionProps = Execution.props(
        planId, executionTimeout = cmd.config.timeout
      )
      shardRegion ! ExecutionDriver.New(jobId, spec, planId, config.trigger, executionProps)

    case response @ ExecutionPlanStarted(`jobId`, _) =>
      log.info("Execution plan for job {} has been started.", jobId)
      cmd.requestor ! response
      mediator ! DistributedPubSubMediator.Unsubscribe(topics.Scheduler, self)
      context.become(shuttingDown)
  }

  def shuttingDown: Receive = {
    case DistributedPubSubMediator.UnsubscribeAck(_) =>
      context.stop(self)
  }

}

private class ExecutionPlanView(shardRegion: ActorRef) extends Actor {

  private[this] var executionPlans = Map.empty[PlanId, Boolean]

  def receive: Receive = {
    case get: GetExecutionPlan =>
      if (executionPlans.contains(get.planId)) {
        shardRegion.tell(get, sender())
      } else {
        sender() ! None
      }

    case GetExecutionPlans =>
      sender() ! executionPlans.keySet

    case evt: ExecutionDriver.Created =>
      executionPlans += (evt.planId -> true)

    case evt: ExecutionPlanFinished =>
      executionPlans += (evt.planId -> false)
  }

}