package io.chronos.scheduler

import java.util.UUID

import akka.actor.{ActorLogging, Props}
import akka.cluster.Cluster
import akka.persistence.PersistentActor
import io.chronos.protocol.{JobAccepted, JobRejected, RegisterJob, RegistryEvent}
import io.chronos.resolver.ModuleResolver
import io.chronos.scheduler.store.RegistryStore

/**
 * Created by aalonsodominguez on 10/08/15.
 */
object Registry {

  def props(moduleResolver: ModuleResolver): Props =
    Props(classOf[Registry], moduleResolver)

}

class Registry(moduleResolver: ModuleResolver) extends PersistentActor with ActorLogging {

  private var store = RegistryStore.empty

  override def persistenceId: String = Cluster(context.system).selfRoles.find(_.startsWith("backend-")) match {
    case Some(role) => role + "-registry"
    case None       => "registry"
  }

  override def receiveRecover: Receive = {
    case event: RegistryEvent =>
      store = store.update(event)
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
            store = store.update(event)
            sender() ! event
          }
      }
  }

}
