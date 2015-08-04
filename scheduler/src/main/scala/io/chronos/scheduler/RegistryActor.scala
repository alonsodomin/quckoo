package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, Props}
import akka.contrib.pattern.ClusterReceptionistExtension
import io.chronos.protocol._
import io.chronos.resolver.ModuleResolver

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object RegistryActor {

  def props(jobRegistry: Registry, moduleResolver: ModuleResolver): Props =
    Props(classOf[RegistryActor], jobRegistry, moduleResolver)

}

class RegistryActor(jobRegistry: Registry, moduleResolver: ModuleResolver)
  extends Actor with ActorLogging {

  ClusterReceptionistExtension(context.system).registerService(self)

  def receive = {
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
          jobRegistry.registerJob(jobSpec)
          log.info("Job spec has been registered. jobId={}, name={}", jobSpec.id, jobSpec.displayName)
          sender() ! JobAccepted(jobSpec.id)
      }

    case GetJob(jobId) =>
      sender() ! jobRegistry.getJob(jobId)

    case GetJobs =>
      jobRegistry.getJobs.foreach { sender ! _ }

  }

}
