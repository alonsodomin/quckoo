package io.kairos.ui

import io.kairos.ui.protocol.JobSpecDetails

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 17/10/2015.
 */
trait RegistryApi {

  def getJobs()(implicit ec: ExecutionContext): Future[Seq[JobSpecDetails]]

}
