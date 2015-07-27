package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, Props}
import akka.contrib.pattern.ClusterReceptionistExtension
import io.chronos.protocol.RegistryProtocol
import io.chronos.resolver.JobModuleResolver

import scala.util.{Failure, Success}

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object RegistryActor {

  def props(jobRegistry: JobRegistry, moduleResolver: JobModuleResolver): Props =
    Props(classOf[RegistryActor], jobRegistry, moduleResolver)

}

class RegistryActor(jobRegistry: JobRegistry, moduleResolver: JobModuleResolver) extends Actor with ActorLogging {
  import RegistryProtocol._
  import context.dispatcher

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
          jobRegistry.registerJobSpec(jobSpec).onComplete {
            case Success(jobId) =>
              log.info("Job spec has been registered. jobId={}, name={}", jobId, jobSpec.displayName)
              sender() ! JobAccepted(jobId)

            case Failure(cause) =>
              log.error("Couldn't register job spec for given module. moduleId={}", jobSpec.moduleId)
              sender() ! JobRejected(Right(cause))
          }
      }

    case GetRegisteredJobs =>
      jobRegistry.availableJobSpecs.onComplete {
        case Success(jobSpecs) =>
          sender() ! RegisteredJobs(jobSpecs)
        case Failure(cause) =>
          log.error(cause, "Error obtaining available job specs from the registry.")
      }

  }

}
