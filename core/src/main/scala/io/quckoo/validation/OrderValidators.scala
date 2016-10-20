package io.quckoo.validation

import scalaz._
import Scalaz._

import Violation._

/**
  * Created by alonsodomin on 21/10/2016.
  */
trait OrderValidators {

  def greaterThan[A](value: A)(implicit order: Order[A]): Validator[A] =
    Validator[Id, A](a => order.greaterThan(a, value), a => GreaterThan[A](value, a))

  def lessThan[A](value: A)(implicit order: Order[A]): Validator[A] =
    Validator[Id, A](a => order.lessThan(a, value), a => LessThan[A](value, a))

}
