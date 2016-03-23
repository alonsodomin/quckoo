package io.quckoo.console.client.validation

import io.quckoo.fault.Fault

/**
  * Created by alonsodomin on 01/03/2016.
  */
case class ValidatedField[A](value: Option[A] = None, errors: List[Fault] = List()) {

  def valid = value.isDefined && errors.isEmpty

}
