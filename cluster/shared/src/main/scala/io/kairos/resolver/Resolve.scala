package io.kairos.resolver

import io.kairos.id.ArtifactId
import io.kairos.protocol._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 23/01/2016.
  */
trait Resolve {

  def apply(artifactId: ArtifactId, download: Boolean)(implicit ec: ExecutionContext): Future[ResolutionResult]

  def res(artifactId: ArtifactId, download: Boolean)(implicit ec: ExecutionContext): Future[Response[Artifact]]

}
