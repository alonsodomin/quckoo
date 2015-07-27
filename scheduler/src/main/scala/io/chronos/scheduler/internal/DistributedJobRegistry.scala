package io.chronos.scheduler.internal

import io.chronos.JobSpec
import io.chronos.id._
import io.chronos.scheduler.JobRegistry
import org.apache.ignite.Ignite
import org.apache.ignite.lang.{IgniteFuture, IgniteInClosure}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

/**
 * Created by aalonsodominguez on 26/07/15.
 */
trait DistributedJobRegistry extends JobRegistry {
  protected implicit val ignite: Ignite

  private val jobSpecCache = ignite.getOrCreateCache[JobId, JobSpec]("jobSpecCache").withAsync()

  override def availableJobSpecs(implicit ec: ExecutionContext): Future[Seq[JobSpec]] = ???

  override def specById(jobId: JobId)(implicit ec: ExecutionContext): Future[Option[JobSpec]] = {
    implicit val async = jobSpecCache
    jobSpecCache.get(jobId)

    jobSpecCache.future[JobSpec]().map(Option(_))
  }

  override def registerJobSpec(jobSpec: JobSpec)(implicit ec: ExecutionContext): Future[JobId] = {
    val promise = Promise[JobId]()
    jobSpecCache.put(jobSpec.id, jobSpec)

    val internalFuture = jobSpecCache.future()
    internalFuture.listen(new IgniteInClosure[IgniteFuture[Nothing]] {
      override def apply(e: IgniteFuture[Nothing]): Unit =
        promise.complete(Success(jobSpec.id))
    })
    promise.future
  }

}
