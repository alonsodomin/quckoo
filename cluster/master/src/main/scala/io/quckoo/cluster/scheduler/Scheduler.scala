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
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.persistence.query.scaladsl.{AllPersistenceIdsQuery, EventsByPersistenceIdQuery}

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

  def props(registry: ActorRef, queueProps: Props)(implicit timeSource: TimeSource) =
    Props(classOf[Scheduler], registry, queueProps, timeSource)

}

class Scheduler(registry: ActorRef, queueProps: Props)(implicit timeSource: TimeSource)
    extends Actor with ActorLogging {

  import Scheduler._

  ClusterClientReceptionist(context.system).registerService(self)

  private[this] val monitor = context.actorOf(TaskQueueMonitor.props, "monitor")
  private[this] val taskQueue = context.actorOf(queueProps, "queue")
  private[this] val shardRegion = ClusterSharding(context.system).start(
    ExecutionDriver.ShardName,
    entityProps     = ExecutionDriver.props,
    settings        = ClusterShardingSettings(context.system).withRememberEntities(true),
    extractEntityId = ExecutionDriver.idExtractor,
    extractShardId  = ExecutionDriver.shardResolver
  )
  private[this] val executionPlanIndex = context.actorOf(
    ExecutionPlanIndex.props(shardRegion), "executionPlanIndex"
  )

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      val handler = context.actorOf(jobFetcherProps(cmd.jobId, sender(), cmd), "handler")
      registry.tell(GetJob(cmd.jobId), handler)

    case cmd @ CreateExecutionDriver(_, config, _) =>
      val planId = UUID.randomUUID()
      val props = factoryProps(config.jobId, planId, cmd, shardRegion)
      log.debug("Found enabled job {}. Initializing a new execution plan for it.", config.jobId)
      context.actorOf(props, s"execution-driver-factory-$planId")

    case cmd: SchedulerReadCommand =>
      executionPlanIndex forward cmd

    case msg: WorkerMessage =>
      taskQueue forward msg
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
    case (`jobId`, spec: JobSpec) =>
      if (!spec.disabled) {
        // create execution plan
        context.parent ! CreateExecutionDriver(spec, config, requestor)
      } else {
        log.info("Found job {} in the registry but is not enabled.", jobId)
        requestor ! JobNotEnabled(jobId)
      }
      context.stop(self)

    case JobNotFound(`jobId`) =>
      log.info("No enabled job with id {} could be retrieved.", jobId)
      requestor ! JobNotFound(jobId)
      context.stop(self)
  }

  override def unhandled(message: Any): Unit = {
    log.warning("Unexpected message {} received when fetching job {}.", message, jobId)
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
