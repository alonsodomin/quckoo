package io.quckoo.validation

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 23/10/2016.
  */
trait AnyValidators {

  def any[A]: Validator[A] = Validator.accept[Id, A]

}
