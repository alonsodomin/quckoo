package io.chronos.scheduler.internal.cache

import java.util

import com.hazelcast.core.HazelcastInstance
import io.chronos.JobSpec
import io.chronos.id._

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 08/08/15.
 */
object JobCache {

  type JobEntry = (JobId, JobSpec)

}

class JobCache(grid: HazelcastInstance) extends DistributedCache[JobId, JobSpec] {
  import JobCache._

  private lazy val jobSpecCache = grid.getMap[JobId, JobSpec]("jobSpecCache")

  def get(jobId: JobId): Option[JobSpec] =
    Option(jobSpecCache.get(jobId))

  def apply(jobId: JobId): JobSpec =
    get(jobId).get

  def +=(jobSpec: JobSpec): JobId = {
    jobSpecCache.put(jobSpec.id, jobSpec)
    jobSpec.id
  }

  def toTraversable: Traversable[JobEntry] = {
    val traversable = new Traversable[JobEntry] {
      val values: util.Collection[util.Map.Entry[JobId, JobSpec]] = jobSpecCache.entrySet()

      override def foreach[U](f: JobEntry => U) =
        for (entry <- values) f(entry.getKey, entry.getValue)
    }
    traversable.view
  }

}
