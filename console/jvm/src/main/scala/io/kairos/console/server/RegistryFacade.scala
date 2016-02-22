package io.kairos.console.server

import io.kairos.id.JobId
import io.kairos.{JobSpec, Validated}

import scala.concurrent.Future

/**
  * Created by alonsodomin on 13/12/2015.
  */
trait RegistryFacade {

  def fetchJob(jobId: JobId): Future[Option[JobSpec]]

  def registerJob(jobSpec: JobSpec): Future[Validated[JobId]]

  def registeredJobs: Future[Map[JobId, JobSpec]]

}
