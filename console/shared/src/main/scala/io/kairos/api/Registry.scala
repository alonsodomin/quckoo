package io.kairos.api

import io.kairos._
import io.kairos.id.JobId
import io.kairos.protocol.RegistryProtocol.{JobDisabled, JobEnabled}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 13/12/2015.
  */
trait Registry {

  def enableJob(jobId: JobId)(implicit ec: ExecutionContext): Future[JobEnabled]

  def disableJob(jobId: JobId)(implicit ec: ExecutionContext): Future[JobDisabled]

  def fetchJob(jobId: JobId)(implicit ec: ExecutionContext): Future[Option[JobSpec]]

  def registerJob(jobSpec: JobSpec)(implicit ec: ExecutionContext): Future[Validated[JobId]]

  def fetchJobs(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]]

}