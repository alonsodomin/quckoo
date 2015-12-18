package io.kairos.protocol

/**
  * Created by alonsodomin on 18/12/2015.
  */
object ResolutionFailed {

  type JobRejectedCause = Either[ResolutionFailed, Throwable]

}

case class ResolutionFailed(unresolvedDependencies: Seq[String])
