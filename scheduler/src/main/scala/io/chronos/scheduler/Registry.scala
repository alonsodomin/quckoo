package io.chronos.scheduler

import java.util.UUID

import akka.actor.{ActorLogging, Props}
import akka.cluster.Cluster
import akka.persistence.PersistentActor
import io.chronos.JobSpec
import io.chronos.id._
import io.chronos.protocol._
import io.chronos.resolver.ModuleResolver

/**
 * Created by aalonsodominguez on 10/08/15.
 */
object Registry {

  def props(moduleResolver: ModuleResolver): Props =
    Props(classOf[Registry], moduleResolver)

  sealed trait RegistryCommand
  sealed trait RegistryEvent

  case class GetJob(jobId: JobId) extends RegistryCommand
  case object GetJobs extends RegistryCommand
  case class JobNotEnabled(jobId: JobId)

  case class RegisterJob(job: JobSpec) extends RegistryCommand
  case class JobAccepted(jobId: JobId, job: JobSpec) extends RegistryEvent
  case class JobRejected(cause: JobRejectedCause) extends RegistryEvent

  case class DisableJob(jobId: JobId) extends RegistryCommand
  case class JobDisabled(jobId: JobId) extends RegistryEvent

  object RegistryStore {

    def empty: RegistryStore = new RegistryStore(Map.empty, Map.empty)

  }

  case class RegistryStore private (
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

class Registry(moduleResolver: ModuleResolver) extends PersistentActor with ActorLogging {
  import Registry._

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
      moduleResolver.resolve(jobSpec.moduleId) match {
        case Left(failed) =>
          log.error(
            "Couldn't resolve the job module. jobModuleId={}, unresolved={}",
            jobSpec.moduleId,
            failed.unresolvedDependencies.mkString(",")
          )
          sender() ! JobRejected(Left(failed))

        case Right(modulePackage) =>
          log.debug("Job module has been successfully resolved. jobModuleId={}", jobSpec.moduleId)
          val jobId = UUID.randomUUID()
          persist(JobAccepted(jobId, jobSpec)) { event =>
            log.info("Job spec has been registered. jobId={}, name={}", jobSpec.id, jobSpec.displayName)
            store = store.updated(event)
            sender() ! event
          }
      }

    case DisableJob(jobId) =>
      if (!store.isEnabled(jobId)) {
        sender() ! JobNotEnabled(jobId)
      } else {
        persist(JobDisabled(jobId)) { event =>
          store = store.updated(event)
          sender() ! event
        }
      }

    case GetJob(jobId) =>
      if (store.isEnabled(jobId)) {
        sender() ! store.get(jobId)
      } else {
        sender() ! JobNotEnabled(jobId)
      }

    case GetJobs =>
      sender() ! store.listEnabled
  }

}