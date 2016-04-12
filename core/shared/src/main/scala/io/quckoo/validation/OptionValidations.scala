package io.quckoo.validation

import io.quckoo.fault.{Required, ValidationFault}

import scalaz._

/**
  * Created by alonsodomin on 31/01/2016.
  */
trait OptionValidations {
  import Scalaz._

  def defined[T](t: Option[T])(msg: => String): Validation[ValidationFault, Option[T]] = {
    if (t.isEmpty) Required(msg).failure[Option[T]]
    else t.success[Required]
  }

}
