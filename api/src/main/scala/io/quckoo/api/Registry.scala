package io.quckoo.api

import io.quckoo.auth.AuthInfo
import io.quckoo.{JobSpec, Validated}
import io.quckoo.id.JobId
import io.quckoo.protocol.registry._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 13/12/2015.
  */
trait Registry {

  def enableJob(jobId: JobId)(implicit ec: ExecutionContext, auth: AuthInfo): Future[JobEnabled]

  def disableJob(jobId: JobId)(implicit ec: ExecutionContext, auth: AuthInfo): Future[JobDisabled]

  def fetchJob(jobId: JobId)(implicit ec: ExecutionContext, auth: AuthInfo): Future[Option[JobSpec]]

  def registerJob(jobSpec: JobSpec)(implicit ec: ExecutionContext, auth: AuthInfo): Future[Validated[JobId]]

  def fetchJobs(implicit ec: ExecutionContext, auth: AuthInfo): Future[Map[JobId, JobSpec]]

}
