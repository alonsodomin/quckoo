package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, Props}
import com.hazelcast.core.HazelcastInstance
import io.chronos.protocol.SchedulerProtocol._
import io.chronos.scheduler.runtime.HazelcastJobRegistry

/**
 * Created by aalonsodominguez on 10/07/15.
 */
object Registry {

  def props(hazelcastInstance: HazelcastInstance): Props =
    Props(classOf[Registry], hazelcastInstance)

}

class Registry(hazelcastInstance: HazelcastInstance) extends Actor with ActorLogging {

  private val jobRegistry = new HazelcastJobRegistry(hazelcastInstance)

  override def receive: Receive = {
    case PublishJob(job) =>
      jobRegistry.publishSpec(job)
      log.info("Job spec has been published. jobId={}, name={}", job.id, job.displayName)

    case GetJobSpecs =>
      log.info("Retrieving the available job specs from the registry.")
      sender() ! JobSpecs(jobRegistry.availableSpecs)

    case GetScheduledJobs =>
      sender() ! ScheduledJobs(jobRegistry.scheduledJobs)
  }
}
