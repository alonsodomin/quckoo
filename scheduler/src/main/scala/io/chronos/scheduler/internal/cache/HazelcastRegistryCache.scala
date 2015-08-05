package io.chronos.scheduler.internal.cache

import com.hazelcast.core.HazelcastInstance
import io.chronos.JobSpec
import io.chronos.id._
import io.chronos.scheduler.Registry

/**
 * Created by domingueza on 04/08/15.
 */
trait HazelcastRegistryCache extends Registry {

  val hazelcastInstance: HazelcastInstance

  private lazy val jobSpecCache = hazelcastInstance.getMap[JobId, JobSpec]("jobSpecCache")

  override final def getJob(jobId: JobId): Option[JobSpec] = Option(jobSpecCache.get(jobId))

  override final def registerJob(jobSpec: JobSpec): Unit = jobSpecCache.put(jobSpec.id, jobSpec)

  override final def getJobs: Seq[JobSpec] = {
    // TODO need to find a better way to perform the ordering in here
    val ordering: Ordering[(JobId, JobSpec)] = Ordering.by(_._2.displayName)
    HazelcastTraversable(jobSpecCache, ordering, 50).toStream
  }

}
