package io.kairos.console

import io.kairos.JobSpec
import io.kairos.id.JobId
import io.kairos.protocol.ResolutionFailed

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 17/10/2015.
 */
trait RegistryApi {

  def registerJob(jobSpec: JobSpec)(implicit ex: ExecutionContext): Future[Either[ResolutionFailed, JobId]]

  def enabledJobs(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]]

}
