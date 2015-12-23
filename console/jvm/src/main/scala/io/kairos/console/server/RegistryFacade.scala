package io.kairos.console.server

import io.kairos.JobSpec
import io.kairos.id.JobId

import scala.concurrent.Future

/**
  * Created by alonsodomin on 13/12/2015.
  */
trait RegistryFacade {

  def registeredJobs: Future[Map[JobId, JobSpec]]

}
