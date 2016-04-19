package io.quckoo.cluster.registry

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, RootActorPath, Stash}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.ShardRegion
import akka.persistence.{PersistentActor, RecoveryCompleted}
import io.quckoo.JobSpec
import io.quckoo.cluster.registry.RegistryIndex.IndexJob
import io.quckoo.cluster.topics
import io.quckoo.id.JobId
import io.quckoo.protocol.registry._
import io.quckoo.resolver.Resolver

/**
  * Created by alonsodomin on 15/04/2016.
  */
object JobAggregate {

  final val ShardName      = "JobState"
  final val NumberOfShards = 100

  final val PersistenceIdPrefix = "JobState"

  val idExtractor: ShardRegion.ExtractEntityId = {
    case r: RegisterJob => (JobId(r.job).toString, r)
    case g: GetJob      => (g.jobId.toString, g)
    case d: DisableJob  => (d.jobId.toString, d)
    case e: EnableJob   => (e.jobId.toString, e)
  }

  val shardResolver: ShardRegion.ExtractShardId = {
    case RegisterJob(jobSpec) => (JobId(jobSpec).hashCode % NumberOfShards).toString
    case GetJob(jobId)        => (jobId.hashCode % NumberOfShards).toString
    case DisableJob(jobId)    => (jobId.hashCode % NumberOfShards).toString
    case EnableJob(jobId)     => (jobId.hashCode % NumberOfShards).toString
  }

  def props: Props = Props(classOf[JobAggregate])

}

class JobAggregate extends PersistentActor with ActorLogging with Stash {
  import JobAggregate._

  private[this] val mediator = DistributedPubSub(context.system).mediator
  // Use the artifact resolver located at current node
  private[this] val resolver = context.actorSelection(
    RootActorPath(self.path.address) / "user" / "quckoo" / "registry" / "resolver"
  )
  private[this] var handlerRefCount = 0L
  private[this] var stateDuringRecovery: Option[JobSpec] = None

  override def persistenceId: String = s"$PersistenceIdPrefix-${self.path.name}"

  override def preStart(): Unit =
    context.system.eventStream.publish(IndexJob(persistenceId))

  override def receiveRecover: Receive = {
    case JobAccepted(jobId, jobSpec) =>
      stateDuringRecovery = Some(jobSpec)
      context.become(enabled(jobId, jobSpec))

    case JobRejected(jobId, _, _) =>
      context.parent ! ShardRegion.Passivate(PoisonPill)
      context.become(disposing(jobId))

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
    case RegisterJob(spec) =>
      handlerRefCount += 1
      val handler = context.actorOf(handlerProps(spec, sender()), s"handler-$handlerRefCount")
      resolver.tell(Resolver.Validate(spec.artifactId), handler)

    case msg @ JobAccepted(jobId, jobSpec) =>
      persist(msg) { event =>
        mediator ! DistributedPubSubMediator.Publish(topics.Registry, event)
        sender() ! event
        context.become(enabled(jobId, jobSpec))
        unstashAll()
      }

    case msg: JobRejected =>
      persist(msg) { event =>
        mediator ! DistributedPubSubMediator.Publish(topics.Registry, event)
        sender() ! event
        unstashAll()
        context.parent ! ShardRegion.Passivate(PoisonPill)
        context.become(disposing(msg.jobId))
      }

    case _: GetJob =>
      stash()
  }

  def enabled(jobId: JobId, spec: JobSpec): Receive = {
    case EnableJob(`jobId`) =>
      sender() ! JobEnabled(jobId)

    case DisableJob(`jobId`) =>
      persist(JobDisabled(jobId)) { event =>
        mediator ! DistributedPubSubMediator.Publish(topics.Registry, event)
        sender() ! event
        context.become(disabled(jobId, spec.copy(disabled = true)))
      }

    case GetJob(`jobId`) =>
      sender() ! spec
  }

  def disabled(jobId: JobId, spec: JobSpec): Receive = {
    case DisableJob(`jobId`) =>
      sender() ! JobDisabled(jobId)

    case EnableJob(`jobId`) =>
      persist(JobEnabled(jobId)) { event =>
        mediator ! DistributedPubSubMediator.Publish(topics.Registry, event)
        sender() ! event
        context.become(enabled(jobId, spec.copy(disabled = false)))
      }

    case GetJob(`jobId`) =>
      sender() ! spec
  }

  def disposing(jobId: JobId): Receive = {
    case _: RegistryCommand =>
      sender() ! JobNotFound(jobId)
  }

  private def handlerProps(jobSpec: JobSpec, requestor: ActorRef): Props =
    Props(classOf[JobResolutionHandler], jobSpec, requestor)

}

private class JobResolutionHandler(jobSpec: JobSpec, requestor: ActorRef) extends Actor with ActorLogging {
  import Resolver._

  private val jobId = JobId(jobSpec)

  def receive = {
    case ArtifactResolved(artifact) =>
      log.debug("Job artifact has been successfully resolved. artifactId={}",
        artifact.artifactId)
      reply(JobAccepted(jobId, jobSpec))

    case ResolutionFailed(cause) =>
      log.error("Couldn't validate the job artifact id. " + cause)
      reply(JobRejected(jobId, jobSpec.artifactId, cause))
  }

  private def reply(msg: Any): Unit = {
    context.parent.tell(msg, requestor)
    context.stop(self)
  }

}