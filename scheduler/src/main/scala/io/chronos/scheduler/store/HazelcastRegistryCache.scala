package io.chronos.scheduler.store

import java.util.{Collection => JCollection, Comparator, Map => JMap}

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.query.PagingPredicate
import io.chronos.JobSpec
import io.chronos.id._
import io.chronos.scheduler.Registry

import scala.collection.JavaConversions._

/**
 * Created by domingueza on 04/08/15.
 */
trait HazelcastRegistryCache extends Registry {

  val hazelcastInstance: HazelcastInstance

  private lazy val jobSpecCache = hazelcastInstance.getMap[JobId, JobSpec]("jobSpecCache")

  override def getJob(jobId: JobId): Option[JobSpec] = Option(jobSpecCache.get(jobId))

  override def registerJob(jobSpec: JobSpec): Unit = jobSpecCache.put(jobSpec.id, jobSpec)

  override def getJobs: Seq[JobSpec] = {
    val size = jobSpecCache.size()
    val totalPages = size / 10
    // TODO need to find a better way to perform the ordering in here
    val ordering: Ordering[JMap.Entry[JobId, JobSpec]] = Ordering.by(_.getValue.displayName)
    val pagingPredicate = new PagingPredicate(ordering.asInstanceOf[Comparator[JMap.Entry[_, _]]], 10)

    val traversable = new Traversable[JobSpec] {
      var values: JCollection[JobSpec] = _

      override def foreach[U](f: (JobSpec) => U): Unit = {
        do {
          values = jobSpecCache.values(pagingPredicate)
          for (item <- values) {
            f(item)
          }
          pagingPredicate.nextPage()
        } while (pagingPredicate.getPage < totalPages)
      }
    }
    traversable.view.toStream
  }

}
