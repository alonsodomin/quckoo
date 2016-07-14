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

package io.quckoo.cluster.registry

import akka.actor.{ActorLogging, Props, Stash}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.ShardRegion
import akka.persistence.{PersistentActor, RecoveryCompleted}

import io.quckoo.JobSpec
import io.quckoo.cluster.topics
import io.quckoo.id.JobId
import io.quckoo.protocol.registry._

/**
  * Created by alonsodomin on 15/04/2016.
  */
object PersistentJob {

  final val ShardName      = "JobState"
  final val NumberOfShards = 100

  final val PersistenceIdPrefix = "JobState"

  val idExtractor: ShardRegion.ExtractEntityId = {
    case c: CreateJob   => (c.jobId.toString, c)
    case g: GetJob      => (g.jobId.toString, g)
    case d: DisableJob  => (d.jobId.toString, d)
    case e: EnableJob   => (e.jobId.toString, e)
  }

  val shardResolver: ShardRegion.ExtractShardId = {
    case CreateJob(jobId, _)  => (jobId.hashCode % NumberOfShards).toString
    case GetJob(jobId)        => (jobId.hashCode % NumberOfShards).toString
    case DisableJob(jobId)    => (jobId.hashCode % NumberOfShards).toString
    case EnableJob(jobId)     => (jobId.hashCode % NumberOfShards).toString
  }

  private[registry] final case class CreateJob(jobId: JobId, spec: JobSpec)

  def props: Props = Props(classOf[PersistentJob])

}

class PersistentJob extends PersistentActor with ActorLogging with Stash {
  import PersistentJob._
  import RegistryIndex._

  private[this] val mediator = DistributedPubSub(context.system).mediator
  private[this] var stateDuringRecovery: Option[JobSpec] = None

  override def persistenceId: String = s"$PersistenceIdPrefix-${self.path.name}"

  override def receiveRecover: Receive = {
    case JobAccepted(jobId, jobSpec) =>
      stateDuringRecovery = Some(jobSpec)
      log.debug("Loading job {}...", jobId)
      context.become(enabled(jobId, jobSpec))

    case JobEnabled(jobId) =>
      stateDuringRecovery = stateDuringRecovery.map(_.copy(disabled = false))
      stateDuringRecovery.foreach { state =>
        context.become(enabled(jobId, state))
      }

    case JobDisabled(jobId) =>
      stateDuringRecovery = stateDuringRecovery.map(_.copy(disabled = true))
      stateDuringRecovery.foreach { state =>
        context.become(disabled(jobId, state))
      }

    case RecoveryCompleted =>
      log.debug("Job recovery has finished")
      unstashAll()
      stateDuringRecovery = None
  }

  def receiveCommand: Receive = initialising

  def initialising: Receive = {
    case CreateJob(jobId, jobSpec) =>
      persist(JobAccepted(jobId, jobSpec)) { event =>
        log.info("Job {} has been successfully registered.", jobId)
        //context.system.eventStream.publish(IndexJob(jobId))
        //context.system.eventStream.publish(event)
        mediator ! DistributedPubSubMediator.Publish(topics.Registry, event)
        context.become(enabled(jobId, jobSpec))
        unstashAll()
      }

    case _: GetJob => stash()
  }

  def enabled(jobId: JobId, spec: JobSpec): Receive = {
    def validCommands: Receive = {
      case EnableJob(`jobId`) =>
        sender() ! JobEnabled(jobId)

      case DisableJob(`jobId`) =>
        persist(JobDisabled(jobId)) { event =>
          mediator ! DistributedPubSubMediator.Publish(topics.Registry, event)
          sender() ! event
          context.become(disabled(jobId, spec.copy(disabled = true)))
        }
    }

    validCommands orElse returnJob(jobId, spec)
  }

  def disabled(jobId: JobId, spec: JobSpec): Receive = {
    def validCommands: Receive = {
      case DisableJob(`jobId`) =>
        sender() ! JobDisabled(jobId)

      case EnableJob(`jobId`) =>
        persist(JobEnabled(jobId)) { event =>
          mediator ! DistributedPubSubMediator.Publish(topics.Registry, event)
          sender() ! event
          context.become(enabled(jobId, spec.copy(disabled = false)))
        }
    }

    validCommands orElse returnJob(jobId, spec)
  }

  private[this] def returnJob(jobId: JobId, spec: JobSpec): Receive = {
    case CreateJob(`jobId`, _) =>
      sender() ! JobAccepted(jobId, spec)

    case GetJob(`jobId`) =>
      sender() ! (jobId -> spec)
  }

}
