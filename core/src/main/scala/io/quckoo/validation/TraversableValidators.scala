package io.quckoo.validation

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 21/10/2016.
  */
trait TraversableValidators {

  def nonEmpty[A]: Validator[Traversable[A]] =
    Validator[Id, Traversable[A]](_.nonEmpty, _ => Violation.Empty)

}
