package io.kairos

import scala.concurrent.Future
import scalaz._

/**
  * Created by alonsodomin on 28/12/2015.
  */
package object protocol {

  type Response[+A] = Fault \/ A
  type MyResponse[+A] = EitherT[Future, NonEmptyList[Fault], A]

  type Validated[+A] = ValidationNel[Fault, A]

  type JobRejectedCause = NonEmptyList[Fault]

}
