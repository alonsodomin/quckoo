package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, Props}
import akka.contrib.pattern.ClusterReceptionistExtension
import io.chronos.JobSpec
import io.chronos.id._
import io.chronos.protocol._
import io.chronos.resolver.JobModuleResolver
import org.apache.ignite.Ignite

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object RegistryActor {

  def props(ignite: Ignite, moduleResolver: JobModuleResolver): Props =
    Props(classOf[RegistryActor], ignite, moduleResolver)

}

class RegistryActor(ignite: Ignite, moduleResolver: JobModuleResolver)
  extends Actor with ActorLogging {

  private val jobSpecCache = ignite.getOrCreateCache[JobId, JobSpec]("jobSpecCache")

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

    case GetJobSpec(jobId) =>
      sender() ! Option(jobSpecCache.get(jobId))

    case GetRegisteredJobs =>
      sender() ! RegisteredJobs(
        jobSpecCache.localEntries().map(_.getValue).toSeq
      )

  }

}
