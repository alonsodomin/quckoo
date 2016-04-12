package io.quckoo.validation

import io.quckoo.fault.{NotNull, ValidationFault}

import scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
trait AnyRefValidations {
  import Scalaz._

  def notNull[T <: AnyRef](t: T)(msg: => String): Validation[ValidationFault, T] = {
    if (t == null) NotNull(msg).failure[T]
    else t.success[NotNull]
  }

}
