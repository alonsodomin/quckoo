package io.quckoo.validation

import scalaz._
import Scalaz._

import Violation._

/**
  * Created by alonsodomin on 21/10/2016.
  */
trait OrderValidators {

  def greaterThanK[F[_]: Applicative, A](value: A)(implicit order: Order[A]): ValidatorK[F, A] =
    Validator[F, A](a => Applicative[F].pure(order.greaterThan(a, value)), a => GreaterThan[A](value, a))

  def greaterThan[A: Order](value: A): Validator[A] = greaterThanK[Id, A](value)

  def lessThanK[F[_]: Applicative, A](value: A)(implicit order: Order[A]): ValidatorK[F, A] =
    Validator[F, A](a => Applicative[F].pure(order.lessThan(a, value)), a => LessThan[A](value, a))

  def lessThan[A: Order](value: A): Validator[A] = lessThanK[Id, A](value)

}
