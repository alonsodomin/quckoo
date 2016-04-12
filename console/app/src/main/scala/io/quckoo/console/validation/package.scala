package io.quckoo.console

import io.quckoo.fault.{Required, ValidationFault}

import scalaz._

/**
  * Created by alonsodomin on 21/02/2016.
  */
package object validation {
  import Scalaz._

  type Validator[A] = A => ValidationNel[ValidationFault, String]

  def notEmptyStr(fieldId: String)(str: String): Validation[ValidationFault, String] = {
    if (str.isEmpty) Required(fieldId).failure[String]
    else str.success[ValidationFault]
  }

}
