package io.kairos.console.server

import io.kairos.JobSpec
import io.kairos.id.JobId
import io.kairos.protocol.ResolutionFailed

import scala.concurrent.Future

/**
  * Created by alonsodomin on 13/12/2015.
  */
trait RegistryFacade {

  def registerJob(jobSpec: JobSpec): Future[Either[ResolutionFailed, JobId]]

  def registeredJobs: Future[Map[JobId, JobSpec]]

}
