package io.quckoo.validation

import scalaz._
import Scalaz._

import Violation.Undefined

/**
  * Created by alonsodomin on 21/10/2016.
  */
trait OptionValidators {

  def defined[A]: Validator[Option[A]] =
    Validator[Id, Option[A]](_.isEmpty, _ => Undefined)

}
