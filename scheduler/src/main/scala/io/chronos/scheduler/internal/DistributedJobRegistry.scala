package io.chronos.scheduler.internal

import io.chronos.JobSpec
import io.chronos.id._
import io.chronos.scheduler.JobRegistry
import org.apache.ignite.Ignite

import scala.collection.JavaConversions._
import scala.concurrent._

/**
 * Created by aalonsodominguez on 26/07/15.
 */
trait DistributedJobRegistry extends JobRegistry {
  protected implicit val ignite: Ignite

  private val jobSpecCache = ignite.getOrCreateCache[JobId, JobSpec]("jobSpecCache")

  override def availableJobSpecs(implicit ec: ExecutionContext): Future[Seq[JobSpec]] =
    Future { jobSpecCache.getAll(Set.empty).values().toSeq }

  override def specById(jobId: JobId)(implicit ec: ExecutionContext): Future[Option[JobSpec]] = Future { Option(jobSpecCache.get(jobId)) }

  override def registerJobSpec(jobSpec: JobSpec)(implicit ec: ExecutionContext): Future[JobId] = {
    jobSpecCache.put(jobSpec.id, jobSpec)
    Future { jobSpec.id }
  }

}
