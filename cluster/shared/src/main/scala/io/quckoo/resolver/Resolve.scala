package io.quckoo.resolver

import io.quckoo.fault.ResolutionFault
import io.quckoo.id.ArtifactId

import scala.concurrent.{ExecutionContext, Future}
import scalaz._

/**
  * Created by alonsodomin on 23/01/2016.
  */
trait Resolve {

  def apply(artifactId: ArtifactId, download: Boolean)(implicit ec: ExecutionContext): Future[ValidationNel[ResolutionFault, Artifact]]

}
