package io.kairos.validation

import io.kairos.fault.{NotNull, ValidationFault}
import io.kairos.Validated

import scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
trait AnyRefValidations {
  import Scalaz._

  def notNull[T <: AnyRef](t: T)(msg: => String): Validated[T] = {
    if (t == null) NotNull(msg).failureNel[T]
    else t.successNel[ValidationFault]
  }

}
