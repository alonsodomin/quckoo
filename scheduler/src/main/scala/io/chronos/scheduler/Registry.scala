package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import akka.persistence.PersistentActor
import io.chronos.JobSpec
import io.chronos.id._
import io.chronos.protocol._
import io.chronos.resolver.ModuleResolver.ErrorResolvingModule
import io.chronos.resolver.{JobPackage, ModuleResolver}

/**
 * Created by aalonsodominguez on 10/08/15.
 */
object Registry {
  import RegistryProtocol._

  def props(resolverProps: Props): Props =
    Props(classOf[Registry], resolverProps)

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

}

class Registry(resolverProps: Props) extends PersistentActor with ActorLogging {
  import ModuleResolver._
  import Registry._
  import RegistryProtocol._

  ClusterClientReceptionist(context.system).registerService(self)

  private val moduleResolver = context.actorOf(resolverProps, "moduleResolver")
  
  private var store = RegistryStore.empty

  override def persistenceId: String = Cluster(context.system).selfRoles.find(_.startsWith("backend-")) match {
    case Some(role) => role + "-registry"
    case None       => "registry"
  }

  override def receiveRecover: Receive = {
    case event: RegistryEvent =>
      store = store.updated(event)
      log.info("Replayed registry event. event={}", event)
  }

  override def receiveCommand: Receive = {
    case RegisterJob(jobSpec) =>
      val handler = context.actorOf(Props(classOf[ResolutionHandler], jobSpec, self, sender()))
      moduleResolver.tell(ResolveModule(jobSpec.moduleId, download = false), handler)

    case (jobSpec: JobSpec, _: JobPackage) =>
      log.debug("Job module has been successfully resolved. jobModuleId={}", jobSpec.moduleId)
      //val jobId = UUID.randomUUID()
      persist(JobAccepted(jobSpec.id, jobSpec)) { event =>
        log.info("Job spec has been registered. jobId={}, name={}", jobSpec.id, jobSpec.displayName)
        store = store.updated(event)
        sender() ! event
      }

    case (jobSpec: JobSpec, failed: ResolutionFailed) =>
      log.error(
        "Couldn't resolve the job module. jobModuleId={}, unresolved={}",
        jobSpec.moduleId,
        failed.unresolvedDependencies.mkString(",")
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
  }

}

private class ResolutionHandler(jobSpec: JobSpec, registry: ActorRef, requestor: ActorRef) extends Actor {

  def receive: Receive = {
    case pkg: JobPackage =>
      registry.tell((jobSpec, pkg), requestor)
      context.stop(self)
      
    case failed: ResolutionFailed =>
      registry.tell((jobSpec, failed), requestor)
      context.stop(self)

    case error: ErrorResolvingModule =>
      registry.tell(error, requestor)
      context.stop(self)
  }

}