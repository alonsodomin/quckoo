package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, Props}
import akka.contrib.pattern.ClusterReceptionistExtension
import io.chronos.protocol.SchedulerProtocol._
import io.chronos.resolver.JobModuleResolver
import org.codehaus.plexus.classworlds.ClassWorld

/**
 * Created by aalonsodominguez on 10/07/15.
 */
object Registry {

  def props(jobRegistry: JobRegistry, classWorld: ClassWorld, moduleResolver: JobModuleResolver): Props =
    Props(classOf[Registry], jobRegistry, classWorld, moduleResolver)

}

class Registry(jobRegistry: JobRegistry, implicit val classWorld: ClassWorld, moduleResolver: JobModuleResolver) extends Actor with ActorLogging {

  ClusterReceptionistExtension(context.system).registerService(self)

  override def receive: Receive = {
    case RegisterJob(job) =>
      moduleResolver.resolve(job.moduleId) match {
        case Left(jobPackage) =>
          log.info("Job module {} has been successfully resolved.", job.moduleId)
          jobRegistry.registerJobSpec(job)
          log.info("Job spec has been registered. jobId={}, name={}", job.id, job.displayName)
          sender() ! RegisterJobAck
        case Right(failed) =>
          log.error("Couldn't resolve the jod module {}. Unresolved dependencies: {}", job.moduleId, failed.unresolvedDependencies)
          sender() ! RegisterJobNAck()
      }

    case GetJobSpecs =>
      log.info("Retrieving the available job specs from the registry.")
      sender() ! JobSpecs(jobRegistry.availableJobSpecs)

  }
}
