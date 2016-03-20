package io.kairos.cluster.registry

import akka.actor.{ActorLogging, Props}
import akka.cluster.pubsub.{DistributedPubSubMediator, DistributedPubSub}
import akka.cluster.sharding.ShardRegion
import akka.pattern._
import akka.persistence.{PersistentActor, SnapshotOffer}

import io.kairos.fault.ExceptionThrown
import io.kairos.id._
import io.kairos.protocol._
import io.kairos.resolver.Resolve
import io.kairos.JobSpec

import scala.concurrent.duration._
import scala.util.control.NonFatal
import scalaz._

/**
 * Created by aalonsodominguez on 10/08/15.
 */
object RegistryShard {
  import RegistryProtocol._

  val DefaultSnapshotFrequency = 15 minutes

  def props(resolve: Resolve,
      snapshotFrequency: FiniteDuration = DefaultSnapshotFrequency): Props =
    Props(classOf[RegistryShard], resolve, snapshotFrequency)

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

class RegistryShard(resolve: Resolve, snapshotFrequency: FiniteDuration)
    extends PersistentActor with ActorLogging {

  import Registry._
  import RegistryShard._
  import RegistryProtocol._

  import context.dispatcher
  private val snapshotTask = context.system.scheduler.schedule(
      snapshotFrequency, snapshotFrequency, self, Snap)

  private val mediator = DistributedPubSub(context.system).mediator
  private var store = RegistryStore.empty

  override val persistenceId: String = PersistenceId

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
      resolve(jobSpec.artifactId, download = false) map {

        case Success(_) =>
          log.debug("Job artifact has been successfully resolved. artifactId={}",
              jobSpec.artifactId)
          val jobId = JobId(jobSpec)
          JobAccepted(jobId, jobSpec)

        case Failure(errors) =>
          log.error("Couldn't validate the job artifact id. " + errors)
          JobRejected(jobSpec.artifactId, errors)

      } recover {
        case NonFatal(ex) =>
          JobRejected(jobSpec.artifactId, NonEmptyList(ExceptionThrown(ex)))

      } map { response =>
        persist(response) { event =>
          store = store.updated(event)
          mediator ! DistributedPubSubMediator.Publish(RegistryTopic, event)
        }
        response
      } pipeTo sender()

    case EnableJob(jobId) if store.contains(jobId) =>
      val answer = JobEnabled(jobId)
      if (!store.isEnabled(jobId)) {
        persist(answer) { event =>
          store = store.updated(event)
          mediator ! DistributedPubSubMediator.Publish(RegistryTopic, event)
        }
      }
      sender() ! answer

    case DisableJob(jobId) if store.contains(jobId) =>
      val answer = JobDisabled(jobId)
      if (store.isEnabled(jobId)) {
        persist(answer) { event =>
          store = store.updated(event)
          mediator ! DistributedPubSubMediator.Publish(RegistryTopic, event)
        }
      }
      sender() ! answer

    case GetJob(jobId) =>
      sender() ! store.get(jobId)

    case Snap =>
      saveSnapshot(store)
  }

}