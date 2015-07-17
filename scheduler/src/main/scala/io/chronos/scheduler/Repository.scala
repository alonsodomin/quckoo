package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, Props}
import akka.contrib.pattern.ClusterReceptionistExtension
import io.chronos.protocol.SchedulerProtocol._
import io.chronos.resolver.JobModuleResolver

/**
 * Created by aalonsodominguez on 10/07/15.
 */
object Repository {

  def props(jobRepository: JobRepository, moduleResolver: JobModuleResolver): Props =
    Props(classOf[Repository], jobRepository, moduleResolver)

}

class Repository(jobRepository: JobRepository, moduleResolver: JobModuleResolver) extends Actor with ActorLogging {

  ClusterReceptionistExtension(context.system).registerService(self)

  override def receive: Receive = {
    case RegisterJob(job) =>
      moduleResolver.resolve(job.moduleId) match {
        case Left(jobPackage) =>
          jobRepository.registerJobSpec(job)
          log.info("Job spec has been registered. jobId={}, name={}", job.id, job.displayName)
          sender() ! RegisterJobAck
        case Right(invalidModule) =>
          sender() ! RegisterJobNAck()
      }

    case GetJobSpecs =>
      log.info("Retrieving the available job specs from the registry.")
      sender() ! JobSpecs(jobRepository.availableJobSpecs)

  }
}
