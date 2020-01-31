/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.validation

import cats.{Applicative, Id, Order, Show}

import Violation._

/**
  * Created by alonsodomin on 21/10/2016.
  */
trait OrderValidators {

  def greaterThanK[F[_]: Applicative, A](
      value: A
  )(implicit order: Order[A], show: Show[A]): ValidatorK[F, A] =
    Validator[F, A](
      a => Applicative[F].pure(order.gt(a, value)),
      a => GreaterThan(show.show(value), show.show(a))
    )

  def greaterThan[A: Order: Show](value: A): Validator[A] = greaterThanK[Id, A](value)

  def lessThanK[F[_]: Applicative, A](
      value: A
  )(implicit order: Order[A], show: Show[A]): ValidatorK[F, A] =
    Validator[F, A](
      a => Applicative[F].pure(order.lt(a, value)),
      a => LessThan(show.show(value), show.show(a))
    )

  def lessThan[A: Order: Show](value: A): Validator[A] = lessThanK[Id, A](value)

}
