package io.quckoo.resolver

import io.quckoo._
import io.quckoo.id.ArtifactId

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 23/01/2016.
  */
trait Resolve {

  def apply(artifactId: ArtifactId, download: Boolean)(implicit ec: ExecutionContext): Future[Validated[Artifact]]

}
