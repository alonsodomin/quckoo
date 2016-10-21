package io.quckoo.validation

import io.quckoo.util.IsTraversable

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 21/10/2016.
  */
trait TraversableValidators {

  def nonEmpty[A](implicit ev: IsTraversable[A]): Validator[A] =
    Validator[Id, A](a => ev.subst(a).nonEmpty, _ => Violation.Empty)

}
