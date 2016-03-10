package io.kairos

import scalaz._

/**
  * Created by alonsodomin on 10/03/2016.
  */
package object fault {

  type Faulty[+A] = Fault \/ A
  type Faults = NonEmptyList[Fault]

}
