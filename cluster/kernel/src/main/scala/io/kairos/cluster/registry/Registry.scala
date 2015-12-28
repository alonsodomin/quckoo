package io.kairos.cluster.registry

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import akka.persistence.{PersistentActor, SnapshotOffer}
import io.kairos.JobSpec
import io.kairos.id._
import io.kairos.protocol._
import io.kairos.resolver.Resolver.ErrorResolvingModule
import io.kairos.resolver.{JobPackage, Resolver}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 10/08/15.
 */
object Registry {
  import RegistryProtocol._

  def props(resolver: ActorRef): Props =
    Props(classOf[Registry], resolver)

  val shardName      = "Registry"
  val numberOfShards = 100

  val idExtractor: ShardRegion.ExtractEntityId = {
    case r: RegisterJob => (JobId(r.job).toString, r)
    case g: GetJob      => (g.jobId.toString, g)
    case d: DisableJob  => (d.jobId.toString, d)
  }

  val shardResolver: ShardRegion.ExtractShardId = {
    case RegisterJob(jobSpec) => (JobId(jobSpec).hashCode % numberOfShards).toString
    case GetJob(jobId)        => (jobId.hashCode % numberOfShards).toString
    case DisableJob(jobId)    => (jobId.hashCode % numberOfShards).toString
  }

  private object RegistryStore {

    def empty: RegistryStore = new RegistryStore(Map.empty, Map.empty)

  }

  private case class RegistryStore private (
    private val enabledJobs: Map[JobId, JobSpec],
    private val disabledJobs: Map[JobId, JobSpec]) {

    def get(id: JobId): Option[JobSpec] =
      enabledJobs.get(id)

    def isEnabled(jobId: JobId): Boolean =
      enabledJobs.contains(jobId)

    def listEnabled: Seq[JobSpec] = enabledJobs.values.toSeq

    def updated(event: RegistryEvent): RegistryStore = event match {
      case JobAccepted(jobId, jobSpec) =>
        copy(enabledJobs = enabledJobs + (jobId -> jobSpec))

      case JobDisabled(jobId) =>
        val job = enabledJobs(jobId)
        copy(enabledJobs = enabledJobs - jobId, disabledJobs = disabledJobs + (jobId -> job))

      // Any event other than the previous ones have no impact in the state
      case _ => this
    }

  }

  private case object Snap

}

class Registry(resolver: ActorRef) extends PersistentActor with ActorLogging {
  import Registry._
  import RegistryProtocol._
  import Resolver._
  import context.dispatcher
  private val snapshotTask = context.system.scheduler.schedule(15 minutes, 15 minutes, self, Snap)

  private var store = RegistryStore.empty

  override val persistenceId: String = "registry"

  override def postStop(): Unit = snapshotTask.cancel()

  override def receiveRecover: Receive = {
    case event: RegistryEvent =>
      store = store.updated(event)
      log.info("Replayed registry event. event={}", event)
    case SnapshotOffer(_, snapshot: RegistryStore) =>
      store = snapshot
  }

  override def receiveCommand: Receive = {
    case RegisterJob(jobSpec) =>
      val handler = context.actorOf(Props(classOf[ResolutionHandler], jobSpec, self, sender()))
      resolver.tell(Validate(jobSpec.moduleId), handler)

    case (jobSpec: JobSpec, _: JobPackage) =>
      log.debug("Job module has been successfully resolved. jobModuleId={}", jobSpec.moduleId)
      val jobId = JobId(jobSpec)
      persist(JobAccepted(jobId, jobSpec)) { event =>
        log.info("Job spec has been registered. jobId={}, name={}", jobId, jobSpec.displayName)
        store = store.updated(event)
        sender() ! event
      }

    case (jobSpec: JobSpec, failed: ResolutionFailed) =>
      log.error(
        "Couldn't resolve the job module. jobModuleId={}, description={}",
        jobSpec.moduleId,
        failed.description
      )
      sender() ! JobRejected(jobSpec.moduleId, Left(failed))

    case DisableJob(jobId) =>
      if (!store.isEnabled(jobId)) {
        sender() ! JobNotEnabled(jobId)
      } else {
        persist(JobDisabled(jobId)) { event =>
          store = store.updated(event)
          context.system.eventStream.publish(event)
          sender() ! event
        }
      }

    case GetJob(jobId) =>
      if (store.isEnabled(jobId)) {
        sender() ! store.get(jobId).get
      } else {
        sender() ! JobNotEnabled(jobId)
      }

    case GetJobs =>
      sender() ! store.listEnabled

    case Snap =>
      saveSnapshot(store)
  }

}

private class ResolutionHandler(jobSpec: JobSpec, registry: ActorRef, requestor: ActorRef) extends Actor {

  def receive: Receive = {
    case pkg: JobPackage =>
      reply(jobSpec -> pkg)

    case failed: ResolutionFailed =>
      reply(jobSpec -> failed)

    case error: ErrorResolvingModule =>
      reply(error)
  }

  private def reply(msg: Any): Unit = {
    registry.tell(msg, requestor)
    context.stop(self)
  }

}