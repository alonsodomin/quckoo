package io.kairos.validation

import io.kairos.fault.{Required, ValidationFault}
import io.kairos.Validated

import scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
trait StringValidations {
  import Scalaz._

  def notNullOrEmpty(str: String)(msg: => String): Validated[String] = {
    if (str == null || str.isEmpty) Required(msg).failureNel[String]
    else str.successNel[ValidationFault]
  }

}
