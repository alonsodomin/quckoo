/*
 * Copyright 2015 A. Alonso Dominguez
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
import java.time.Clock

import akka.actor._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.pattern._
import akka.persistence.query.EventEnvelope
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import io.quckoo._
import io.quckoo.api.TopicTag
import io.quckoo.cluster.config.ClusterSettings
import io.quckoo.cluster.journal.QuckooJournal
import io.quckoo.cluster.protocol._
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._

import scala.concurrent._
import scala.concurrent.duration._

/**
  * Created by aalonsodominguez on 16/08/15.
  */
object Scheduler {

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
      spec: JobSpec,
      cmd: ScheduleJob,
      replyTo: ActorRef
  )
  private[scheduler] case class StopExecutionDriver(
      cmd: Any,
      replyTo: ActorRef
  )

  def props(settings: ClusterSettings, journal: QuckooJournal, registry: ActorRef)(
      implicit clock: Clock
  ): Props = {
    val queueProps = TaskQueue.props(settings.taskQueue.maxWorkTimeout)
    Props(new Scheduler(journal, registry, queueProps))
  }

}

class Scheduler(journal: QuckooJournal, registry: ActorRef, queueProps: Props)(
    implicit clock: Clock
) extends Actor with ActorLogging with Stash {

  import Scheduler._
  import SchedulerTagEventAdapter.tags

  //ClusterClientReceptionist(context.system).registerService(self)

  final implicit val materializer = Materializer(context.system)

  private[this] val mediator = DistributedPubSub(context.system).mediator

  context.actorOf(TaskQueueMonitor.props, "monitor")
  private[this] val taskQueue = context.actorOf(queueProps, "queue")
  private[this] val shardRegion = ClusterSharding(context.system).start(
    ExecutionDriver.ShardName,
    entityProps = ExecutionDriver.props(ExecutionDriver.DefaultExecutionLifecycleFactory),
    settings = ClusterShardingSettings(context.system).withRememberEntities(true),
    extractEntityId = ExecutionDriver.idExtractor,
    extractShardId = ExecutionDriver.shardResolver
  )

  private[this] var planIds    = Set.empty[PlanId]
  private[this] var executions = Map.empty[TaskId, TaskExecution]

  override def preStart(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(TopicTag.Scheduler.name, self)

  override def postStop(): Unit =
    mediator ! DistributedPubSubMediator.Unsubscribe(TopicTag.Scheduler.name, self)

  override def receive: Receive = initializing

  private def initializing: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      warmUp()
      context become ready

    case _ => stash()
  }

  private def ready: Receive = {
    case WarmUp.Start =>
      log.info("Scheduler warm up started...")
      sender() ! WarmUp.Ack
      context become warmingUp

    case cmd: ScheduleJob =>
      val handler = context.actorOf(jobFetcherProps(cmd.jobId, sender(), cmd))
      registry.tell(GetJob(cmd.jobId), handler)

    case cmd @ CreateExecutionDriver(_, config, _) =>
      val planId = PlanId(UUID.randomUUID())
      val props  = factoryProps(config.jobId, planId, cmd)
      log.debug("Found enabled job '{}'. Initializing a new execution plan for it.", config.jobId)
      context.actorOf(props, s"execution-driver-factory-$planId")

    case cancel: CancelExecutionPlan =>
      log.debug("Starting execution driver termination process for plan '{}'.", cancel.planId)
      val props = terminatorProps(cancel, sender())
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
        (shardRegion ? GetExecutionPlan(planId))
          .mapTo[ExecutionPlan]
          .map(planId -> _)
      }

      Source(planIds)
        .mapAsync(2)(fetchPlanAsync)
        .runWith(Sink.actorRef(origSender, Status.Success(GetExecutionPlans), Status.Failure(_)))

    case GetTaskExecution(taskId) =>
      if (executions.contains(taskId)) {
        sender() ! executions(taskId)
      } else {
        sender() ! TaskExecutionNotFound(taskId)
      }

    case GetTaskExecutions =>
      Source(executions).runWith(Sink.actorRef(sender(), Status.Success(GetTaskExecutions), Status.Failure(_)))

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

    case WarmUp.Failed(ex) =>
      log.error(ex, "Error during Scheduler warm up.")
      import context.dispatcher
      context.system.scheduler.scheduleOnce(2 seconds, () => warmUp())
      unstashAll()
      context become ready

    case msg: WorkerMessage =>
      taskQueue forward msg

    case _ => stash()
  }

  private def handleEvent(event: Any): Unit = event match {
    case ExecutionPlanStarted(_, planId, _) =>
      log.debug("Indexing execution plan '{}'.", planId)
      planIds += planId

    case TaskScheduled(jobId, planId, task, _) =>
      val execution =
        TaskExecution(planId, task, TaskExecution.Status.Scheduled)
      executions += (task.id -> execution)

    case TaskTriggered(_, _, taskId, _) =>
      executions += (taskId -> executions(taskId).copy(
        status = TaskExecution.Status.InProgress
      ))

    case TaskCompleted(_, _, taskId, _, outcome) =>
      executions += (taskId -> executions(taskId).copy(
        status = TaskExecution.Status.Complete,
        outcome = Some(outcome)
      ))

    case _ =>
  }

  private def warmUp(): Unit = {
    val firstOffset = journal.firstOffset
    val executionPlanEvents =
      journal.read.currentEventsByTag(tags.ExecutionPlan, firstOffset)
    val executionEvents =
      journal.read.currentEventsByTag(tags.Task, firstOffset)

    executionPlanEvents
      .concat(executionEvents)
      .runWith(
        Sink.actorRefWithBackpressure(self, WarmUp.Start, WarmUp.Ack, WarmUp.Completed, WarmUp.Failed)
      )
  }

  private[this] def jobFetcherProps(jobId: JobId, replyTo: ActorRef, config: ScheduleJob): Props =
    Props(new JobFetcher(jobId, replyTo, config))

  private[this] def factoryProps(
      jobId: JobId,
      planId: PlanId,
      createCmd: CreateExecutionDriver
  ): Props =
    Props(new ExecutionDriverFactory(jobId, planId, createCmd, shardRegion))

  private[this] def terminatorProps(cancelCmd: CancelExecutionPlan, replyTo: ActorRef): Props =
    Props(
      new ExecutionDriverTerminator(
        cancelCmd.planId,
        StopExecutionDriver(cancelCmd, replyTo),
        shardRegion
      )
    )

}

private class JobFetcher(jobId: JobId, replyTo: ActorRef, config: ScheduleJob)
    extends Actor with ActorLogging {

  import Scheduler._

  context.setReceiveTimeout(3 seconds)

  def receive: Receive = {
    case spec: JobSpec =>
      if (!spec.disabled) {
        // create execution plan
        context.parent ! CreateExecutionDriver(spec, config, replyTo)
      } else {
        log.info("Found job '{}' in the registry but is not enabled.", jobId)
        replyTo ! JobNotEnabled(jobId)
      }
      context stop self

    case JobNotFound(`jobId`) =>
      log.info("No enabled job with id '{}' could be retrieved.", jobId)
      replyTo ! JobNotFound(jobId)
      context stop self

    case ReceiveTimeout =>
      log.error("Timed out while fetching job '{}' from the registry.", jobId)
      replyTo ! JobNotFound(jobId)
      context stop self
  }

  override def unhandled(message: Any): Unit =
    log.warning("Unexpected message {} received when fetching job '{}'.", message, jobId)

}

private class ExecutionDriverFactory(
    jobId: JobId,
    planId: PlanId,
    createCmd: Scheduler.CreateExecutionDriver,
    shardRegion: ActorRef
) extends Actor with ActorLogging {

  private[this] val mediator = DistributedPubSub(context.system).mediator

  override def preStart(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(TopicTag.Scheduler.name, self)

  def receive: Receive = initializing

  def initializing: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      import createCmd._

      log.debug("Creating execution driver for job '{}' and plan '{}'.", jobId, planId)
      shardRegion ! ExecutionDriver.Initialize(jobId, planId, spec, cmd.trigger, cmd.timeout)

    case response @ ExecutionPlanStarted(`jobId`, _, _) =>
      log.info("Execution plan '{}' for job '{}' has been started.", planId, jobId)
      createCmd.replyTo ! response
      mediator ! DistributedPubSubMediator.Unsubscribe(TopicTag.Scheduler.name, self)
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
    mediator ! Subscribe(TopicTag.Scheduler.name, self)

  def receive: Receive = initializing

  def initializing: Receive = {
    case SubscribeAck(_) =>
      log.debug("Stopping execution plan '{}'.", planId)
      shardRegion ! killCmd.cmd
      context.become(waitingForTermination)
  }

  def waitingForTermination: Receive = {
    case ExecutionPlanFinished(jobId, `planId`, dateTime) =>
      log.debug("Execution plan '{}' has been stopped.", planId)
      killCmd.replyTo ! ExecutionPlanCancelled(jobId, planId, dateTime)
      mediator ! Unsubscribe(TopicTag.Scheduler.name, self)
      context.become(shuttingDown)
  }

  def shuttingDown: Receive = {
    case UnsubscribeAck(_) =>
      context.stop(self)
  }

}
