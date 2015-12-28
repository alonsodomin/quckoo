package io.kairos

/**
  * Created by alonsodomin on 28/12/2015.
  */
package object protocol {

  type JobRejectedCause = Either[ResolutionFailed, Throwable]

}
