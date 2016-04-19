package io.quckoo.cluster.registry

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Status}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.ShardRegion
import akka.persistence.{PersistentActor, SnapshotOffer}
import io.quckoo.JobSpec
import io.quckoo.cluster.topics
import io.quckoo.id._
import io.quckoo.protocol.registry._
import io.quckoo.resolver.Resolver

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 10/08/15.
 */
object RegistryPartition {

  final val DefaultSnapshotFrequency = 15 minutes

  final val PersistenceIdPrefix = "RegistryShard"

  def props(resolver: ActorRef,
      snapshotFrequency: FiniteDuration = DefaultSnapshotFrequency): Props =
    Props(classOf[RegistryPartition], resolver, snapshotFrequency)

  final val ShardName      = "Registry"
  final val NumberOfShards = 100

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

  private object RegistryStore {

    def empty: RegistryStore = new RegistryStore(Map.empty)

  }

  private case class RegistryStore private (
      private val jobs: Map[JobId, JobSpec]) {

    def get(id: JobId): Option[JobSpec] = jobs.get(id)

    def list: Seq[JobSpec] = jobs.values.toSeq

    def contains(jobId: JobId): Boolean =
      jobs.contains(jobId)

    def isEnabled(jobId: JobId): Boolean =
      get(jobId).exists(!_.disabled)

    def updated(event: RegistryEvent): RegistryStore = event match {
      case JobAccepted(jobId, jobSpec) =>
        copy(jobs = jobs + (jobId -> jobSpec))

      case JobEnabled(jobId) if contains(jobId) && !isEnabled(jobId) =>
        copy(jobs = jobs + (jobId -> jobs(jobId).copy(disabled = false)))

      case JobDisabled(jobId) if isEnabled(jobId) =>
        copy(jobs = jobs + (jobId -> jobs(jobId).copy(disabled = true)))

      // Any event other than the previous ones have no impact in the state
      case _ => this
    }

  }

  private case object Snap

}

class RegistryPartition(resolver: ActorRef, snapshotFrequency: FiniteDuration)
    extends PersistentActor with ActorLogging {

  import RegistryPartition._
  import RegistryIndex._
  import context.dispatcher
  private val snapshotTask = context.system.scheduler.schedule(
      snapshotFrequency, snapshotFrequency, self, Snap)

  private val mediator = DistributedPubSub(context.system).mediator
  private var store = RegistryStore.empty
  private[this] var handlerRefCount = 0L

  override val persistenceId: String = s"$PersistenceIdPrefix-${self.path.name}"

  override def preStart(): Unit =
    context.system.eventStream.publish(IndexJob(persistenceId))

  override def postStop(): Unit = snapshotTask.cancel()

  override def receiveRecover: Receive = {
    case event: RegistryEvent =>
      store = store.updated(event)
      log.debug("Replayed registry event. event={}", event)

    case SnapshotOffer(_, snapshot: RegistryStore) =>
      store = snapshot
  }

  override def receiveCommand: Receive = {
    case RegisterJob(jobSpec) =>
      handlerRefCount += 1
      val handler = context.actorOf(handlerProps(jobSpec, sender()), s"handler-$handlerRefCount")
      resolver.tell(Resolver.Validate(jobSpec.artifactId), handler)

    case event: RegistryResolutionEvent =>
      persist(event) { evt =>
        store = store.updated(evt)
        mediator ! DistributedPubSubMediator.Publish(topics.Registry, evt)
        sender() ! evt
      }

    case EnableJob(jobId) if store.contains(jobId) =>
      val answer = JobEnabled(jobId)
      if (!store.isEnabled(jobId)) {
        persist(answer) { event =>
          store = store.updated(event)
          mediator ! DistributedPubSubMediator.Publish(topics.Registry, event)
        }
      }
      sender() ! answer

    case DisableJob(jobId) if store.contains(jobId) =>
      val answer = JobDisabled(jobId)
      if (store.isEnabled(jobId)) {
        persist(answer) { event =>
          store = store.updated(event)
          mediator ! DistributedPubSubMediator.Publish(topics.Registry, event)
        }
      }
      sender() ! answer

    case GetJob(jobId) =>
      val jobSpec = store.get(jobId)
      jobSpec.foreach(sender() ! _)
      sender() ! Status.Success(())

    case Snap =>
      saveSnapshot(store)
  }

  def handlerProps(jobSpec: JobSpec, requestor: ActorRef): Props =
    Props(classOf[ResolutionHandler], jobSpec, requestor)

}

private class ResolutionHandler(jobSpec: JobSpec, requestor: ActorRef) extends Actor with ActorLogging {
  import Resolver._

  private val jobId = JobId(jobSpec)

  def receive = {
    case ArtifactResolved(artifact) =>
      log.debug("Job artifact has been successfully resolved. artifactId={}",
        artifact.artifactId)
      reply(JobAccepted(JobId(jobSpec), jobSpec))

    case ResolutionFailed(cause) =>
      log.error("Couldn't validate the job artifact id. " + cause)
      reply(JobRejected(jobId, jobSpec.artifactId, cause))
  }

  private def reply(msg: Any): Unit = {
    context.parent.tell(msg, requestor)
    context.stop(self)
  }

}