package io.kairos

import scalaz.NonEmptyList

/**
  * Created by alonsodomin on 28/12/2015.
  */
package object protocol {

  type JobRejectedCause = NonEmptyList[Error]

}
