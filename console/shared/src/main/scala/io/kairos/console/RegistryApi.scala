package io.kairos.console

import io.kairos.JobSpec
import io.kairos.id.JobId

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 17/10/2015.
 */
trait RegistryApi {

  def getJobs()(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]]

}
