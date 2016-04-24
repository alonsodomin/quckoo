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
object JobState {

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

  final case class CreateJob(jobId: JobId, spec: JobSpec)

  def props: Props = Props(classOf[JobState])

}

class JobState extends PersistentActor with ActorLogging with Stash {
  import JobState._
  import RegistryIndex._

  private[this] val mediator = DistributedPubSub(context.system).mediator
  private[this] var stateDuringRecovery: Option[JobSpec] = None

  override def persistenceId: String = s"$PersistenceIdPrefix-${self.path.name}"

  override def preStart(): Unit =
    context.system.eventStream.publish(IndexJob(persistenceId))

  override def receiveRecover: Receive = {
    case JobAccepted(jobId, jobSpec) =>
      stateDuringRecovery = Some(jobSpec)
      log.debug("Recreated job {} with specification: {}", jobId, jobSpec)
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
      stateDuringRecovery = None
  }

  def receiveCommand: Receive = initialising

  def initialising: Receive = {
    case CreateJob(jobId, jobSpec) =>
      persist(JobAccepted(jobId, jobSpec)) { event =>
        mediator ! DistributedPubSubMediator.Publish(topics.Registry, event)
        sender() ! event
        context.become(enabled(jobId, jobSpec))
        unstashAll()
      }

    case _: GetJob =>
      stash()
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

  private def returnJob(jobId: JobId, spec: JobSpec): Receive = {
    case GetJob(`jobId`) =>
      sender() ! (jobId -> spec)
  }

}