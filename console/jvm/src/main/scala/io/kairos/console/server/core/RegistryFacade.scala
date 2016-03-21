package io.kairos.console.server.core

import io.kairos._
import io.kairos.id.JobId
import io.kairos.protocol.RegistryProtocol.{JobDisabled, JobEnabled}

import scala.concurrent.Future

/**
  * Created by alonsodomin on 13/12/2015.
  */
trait RegistryFacade {

  def enableJob(jobId: JobId): Future[JobEnabled]

  def disableJob(jobId: JobId): Future[JobDisabled]

  def fetchJob(jobId: JobId): Future[Option[JobSpec]]

  def registerJob(jobSpec: JobSpec): Future[Validated[JobId]]

  def registeredJobs: Future[Map[JobId, JobSpec]]

}
