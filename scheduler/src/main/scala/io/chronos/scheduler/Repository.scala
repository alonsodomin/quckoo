package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, Props}
import akka.contrib.pattern.ClusterReceptionistExtension
import io.chronos.JobRepository
import io.chronos.protocol.SchedulerProtocol._

/**
 * Created by aalonsodominguez on 10/07/15.
 */
object Repository {

  def props(jobRepository: JobRepository): Props =
    Props(classOf[Repository], jobRepository)

}

class Repository(jobRepository: JobRepository) extends Actor with ActorLogging {

  ClusterReceptionistExtension(context.system).registerService(self)

  override def receive: Receive = {
    case PublishJob(job) =>
      jobRepository.publishSpec(job)
      log.info("Job spec has been published. jobId={}, name={}", job.id, job.displayName)
      sender() ! PublishJobAck

    case GetJobSpecs =>
      log.info("Retrieving the available job specs from the registry.")
      sender() ! JobSpecs(jobRepository.availableSpecs)

  }
}
