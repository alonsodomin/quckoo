/*
 * Copyright 2016 Antonio Alonso Dominguez
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

import scalaz._
import Scalaz._

import Violation._

/**
  * Created by alonsodomin on 21/10/2016.
  */
trait OrderValidators {

  def greaterThanK[F[_]: Applicative, A](value: A)(implicit order: Order[A],
                                                   show: Show[A]): ValidatorK[F, A] = {
    Validator[F, A](
      a => Applicative[F].pure(order.greaterThan(a, value)),
      a => GreaterThan(show.shows(value), show.shows(a)))
  }

  def greaterThan[A: Order: Show](value: A): Validator[A] = greaterThanK[Id, A](value)

  def lessThanK[F[_]: Applicative, A](value: A)(implicit order: Order[A],
                                                show: Show[A]): ValidatorK[F, A] =
    Validator[F, A](
      a => Applicative[F].pure(order.lessThan(a, value)),
      a => LessThan(show.shows(value), show.shows(a)))

  def lessThan[A: Order: Show](value: A): Validator[A] = lessThanK[Id, A](value)

}
