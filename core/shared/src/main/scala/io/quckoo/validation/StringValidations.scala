package io.quckoo.validation

import io.quckoo.fault.{Required, ValidationFault}
import io.quckoo.Validated

import scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
trait StringValidations {
  import Scalaz._

  def notNullOrEmpty(str: String)(msg: => String): Validation[ValidationFault, String] = {
    if (str == null || str.isEmpty) Required(msg).failure[String]
    else str.success[Required]
  }

}
