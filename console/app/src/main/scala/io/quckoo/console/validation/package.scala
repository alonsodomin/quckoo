package io.quckoo.console

import io.quckoo._
import io.quckoo.fault.{Required, Fault}

import scalaz._

/**
  * Created by alonsodomin on 21/02/2016.
  */
package object validation {
  import Scalaz._

  type Validator[A] = A => Validated[A]

  def notEmptyStr(fieldId: String)(str: String): Validated[String] = {
    if (str.isEmpty) Required(fieldId).asInstanceOf[Fault].failureNel[String]
    else str.successNel[Fault]
  }

}
