/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster.scheduler

import java.util.UUID

import akka.actor._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.pattern._
import akka.persistence.query.EventEnvelope
import akka.persistence.query.scaladsl.CurrentEventsByTagQuery
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import io.quckoo.{ExecutionPlan, JobSpec}
import io.quckoo.cluster.protocol._
import io.quckoo.cluster.{QuckooClusterSettings, topics}
import io.quckoo.id._
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.time.TimeSource

import scala.concurrent._
import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Scheduler {

  type Journal = CurrentEventsByTagQuery
  type PlanIndexPropsProvider = ActorRef => Props

  private[scheduler] object WarmUp {
    case object Start
    case object Ack
    case object Completed
    final case class Failed(exception: Throwable)
  }

  sealed trait Signal
  case object Ready extends Signal

  private[scheduler] case class CreateExecutionDriver(
    spec: JobSpec, cmd: ScheduleJob, replyTo: ActorRef
  )
  private[scheduler] case class StopExecutionDriver(
    cmd: Any, replyTo: ActorRef
  )

  def props(settings: QuckooClusterSettings, journal: Journal, registry: ActorRef)
           (implicit actorSystem: ActorSystem, timeSource: TimeSource): Props = {
    val queueProps = TaskQueue.props(settings.queueMaxWorkTimeout)
    Props(classOf[Scheduler], journal, registry, queueProps, timeSource)
  }

}

class Scheduler(journal: Scheduler.Journal, registry: ActorRef, queueProps: Props)
               (implicit timeSource: TimeSource)
    extends Actor with ActorLogging with Stash {

  import Scheduler._
  import SchedulerTagEventAdapter.tags

  ClusterClientReceptionist(context.system).registerService(self)

  final implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(context.system), "registry"
  )

  private[this] val monitor = context.actorOf(TaskQueueMonitor.props, "monitor")
  private[this] val taskQueue = context.actorOf(queueProps, "queue")
  private[this] val shardRegion = ClusterSharding(context.system).start(
    ExecutionDriver.ShardName,
    entityProps     = ExecutionDriver.props,
    settings        = ClusterShardingSettings(context.system).withRememberEntities(true),
    extractEntityId = ExecutionDriver.idExtractor,
    extractShardId  = ExecutionDriver.shardResolver
  )

  private[this] var planIds = Set.empty[PlanId]
  private[this] var taskIds = Set.empty[TaskId]

  override def preStart(): Unit = warmUp()

  override def receive: Receive = ready

  private def ready: Receive = {
    case WarmUp.Start =>
      log.info("Scheduler warm up started...")
      sender() ! WarmUp.Ack
      context become warmingUp

    case cmd: ScheduleJob =>
      val handler = context.actorOf(jobFetcherProps(cmd.jobId, sender(), cmd))
      registry.tell(GetJob(cmd.jobId), handler)

    case cmd @ CreateExecutionDriver(_, config, _) =>
      val planId = UUID.randomUUID()
      val props = factoryProps(config.jobId, planId, cmd)
      log.debug("Found enabled job {}. Initializing a new execution plan for it.", config.jobId)
      context.actorOf(props, s"execution-driver-factory-$planId")

    case cancel: CancelExecutionPlan =>
      log.debug("Starting execution driver termination process. planId={}", cancel.planId)
      val props = terminatorProps(cancel)
      context.actorOf(props, s"execution-driver-terminator-${cancel.planId}")

    case get @ GetExecutionPlan(planId) =>
      if (planIds.contains(planId)) {
        shardRegion forward get
      } else {
        sender() ! ExecutionPlanNotFound(planId)
      }

    case GetExecutionPlans =>
      val origSender = sender()

      def fetchPlanAsync(planId: PlanId): Future[(PlanId, ExecutionPlan)] = {
        import context.dispatcher
        implicit val timeout = Timeout(2 seconds)
        (shardRegion ? GetExecutionPlan(planId)).mapTo[ExecutionPlan].map(planId -> _)
      }

      Source(planIds).
        mapAsync(2)(fetchPlanAsync).
        runWith(Sink.actorRef(origSender, Status.Success(GetExecutionPlans)))

    /*case GetTasks =>
      executionPlanIndex forward GetTasks*/

    case msg: WorkerMessage =>
      taskQueue forward msg

    case event: SchedulerEvent =>
      handleEvent(event)
  }

  private def warmingUp: Receive = {
    case EventEnvelope(_, _, _, event) =>
      handleEvent(event)
      sender() ! WarmUp.Ack

    case WarmUp.Completed =>
      log.info("Scheduler warm up finished.")
      context.system.eventStream.publish(Ready)
      unstashAll()
      context become ready

    case _: SchedulerCommand => stash()
  }

  private def handleEvent(event: Any): Unit = event match {
    case ExecutionPlanStarted(_, planId) =>
      log.debug("Indexing execution plan {}", planId)
      planIds += planId

    case TaskScheduled(_, _, taskId) =>
      log.debug("Indexing task {}", taskId)
      taskIds += taskId

    case _ =>
  }

  private def warmUp(): Unit = {
    val executionPlanEvents = journal.currentEventsByTag(tags.ExecutionPlan, 0)
    val taskEvents = journal.currentEventsByTag(tags.Task, 0)

    executionPlanEvents.merge(taskEvents).
      runWith(Sink.actorRefWithAck(self, WarmUp.Start, WarmUp.Ack, WarmUp.Completed, WarmUp.Failed))
  }

  private[this] def jobFetcherProps(jobId: JobId, requestor: ActorRef, config: ScheduleJob): Props =
    Props(classOf[JobFetcher], jobId, requestor, config)

  private[this] def factoryProps(jobId: JobId, planId: PlanId, createCmd: CreateExecutionDriver): Props =
    Props(classOf[ExecutionDriverFactory], jobId, planId, createCmd, shardRegion)

  private[this] def terminatorProps(cancelCmd: CancelExecutionPlan): Props =
    Props(classOf[ExecutionDriverTerminator], cancelCmd.planId, StopExecutionDriver(cancelCmd, sender()), shardRegion)

}

private class JobFetcher(jobId: JobId, requestor: ActorRef, config: ScheduleJob)
    extends Actor with ActorLogging {

  import Scheduler._

  context.setReceiveTimeout(3 seconds)

  def receive: Receive = {
    case spec: JobSpec =>
      if (!spec.disabled) {
        // create execution plan
        context.parent ! CreateExecutionDriver(spec, config, requestor)
      } else {
        log.info("Found job {} in the registry but is not enabled.", jobId)
        requestor ! JobNotEnabled(jobId)
      }
      context stop self

    case JobNotFound(`jobId`) =>
      log.info("No enabled job with id {} could be retrieved.", jobId)
      requestor ! JobNotFound(jobId)
      context stop self

    case ReceiveTimeout =>
      log.error("Timed out while fetching job {} from the registry.", jobId)
      requestor ! JobNotFound(jobId)
      context stop self
  }

  override def unhandled(message: Any): Unit = {
    log.warning("Unexpected message {} received when fetching job {}.", message, jobId)
  }

}

private class ExecutionDriverFactory(
    jobId: JobId,
    planId: PlanId,
    createCmd: Scheduler.CreateExecutionDriver,
    shardRegion: ActorRef)
  extends Actor with ActorLogging {

  private[this] val mediator = DistributedPubSub(context.system).mediator

  override def preStart(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(topics.Scheduler, self)

  def receive: Receive = initializing

  def initializing: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      import createCmd._

      log.debug("Starting execution plan for job {}.", jobId)
      val executionProps = Execution.props(
        planId, executionTimeout = cmd.timeout
      )
      shardRegion ! ExecutionDriver.New(jobId, spec, planId, cmd.trigger, executionProps)

    case response @ ExecutionPlanStarted(`jobId`, _) =>
      log.info("Execution plan for job {} has been started.", jobId)
      context.parent ! response
      createCmd.replyTo ! response
      mediator ! DistributedPubSubMediator.Unsubscribe(topics.Scheduler, self)
      context.become(shuttingDown)
  }

  def shuttingDown: Receive = {
    case DistributedPubSubMediator.UnsubscribeAck(_) =>
      context.stop(self)
  }

}

private class ExecutionDriverTerminator(
    planId: PlanId,
    killCmd: Scheduler.StopExecutionDriver,
    shardRegion: ActorRef
  ) extends Actor with ActorLogging {

  import DistributedPubSubMediator._

  private[this] val mediator = DistributedPubSub(context.system).mediator

  override def preStart(): Unit =
    mediator ! Subscribe(topics.Scheduler, self)

  def receive = initializing

  def initializing: Receive = {
    case SubscribeAck(_) =>
      log.debug("Stopping execution plan. planId={}", planId)
      shardRegion ! killCmd.cmd
      context.become(waitingForTermination)
  }

  def waitingForTermination: Receive = {
    case response @ ExecutionPlanFinished(_, `planId`) =>
      log.debug("Execution plan has been stopped. planId={}", planId)
      killCmd.replyTo ! response
      mediator ! Unsubscribe(topics.Scheduler, self)
      context.become(shuttingDown)
  }

  def shuttingDown: Receive = {
    case UnsubscribeAck(_) =>
      context.stop(self)
  }

}
