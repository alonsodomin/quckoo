package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, Props}
import akka.contrib.pattern.ClusterReceptionistExtension
import com.hazelcast.core.HazelcastInstance
import io.chronos.JobSpec
import io.chronos.id._
import io.chronos.protocol._
import io.chronos.resolver.JobModuleResolver

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object RegistryActor {

  def props(hazelcastInstance: HazelcastInstance, moduleResolver: JobModuleResolver): Props =
    Props(classOf[RegistryActor], hazelcastInstance, moduleResolver)

}

class RegistryActor(hazelcastInstance: HazelcastInstance, moduleResolver: JobModuleResolver)
  extends Actor with ActorLogging {

  private val jobSpecCache = hazelcastInstance.getMap[JobId, JobSpec]("jobSpecCache")

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
          jobSpecCache.put(jobSpec.id, jobSpec)
          log.info("Job spec has been registered. jobId={}, name={}", jobSpec.id, jobSpec.displayName)
          sender() ! JobAccepted(jobSpec.id)
      }

    case GetJob(jobId) =>
      sender() ! Option(jobSpecCache.get(jobId))

    case GetJobs =>
      sender() ! jobSpecCache.entrySet().map(_.getValue).toSeq

  }

}
