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

/**
  * Created by alonsodomin on 23/10/2016.
  */
trait AnyValidators {

  def anyK[F[_]: Applicative, A]: ValidatorK[F, A] = Validator.accept[F, A]

  def any[A]: Validator[A] = anyK[Id, A]

  def memberOfK[F[_]: Applicative, A](set: Set[A])(implicit show: Show[A]): ValidatorK[F, A] =
    Validator[F, A](
      a => Applicative[F].pure(set.contains(a)),
      a => Violation.MemberOf(set.map(show.shows), show.shows(a))
    )

  def memberOf[A: Show](set: Set[A]): Validator[A] =
    memberOfK[Id, A](set)

  def equalToK[F[_]: Applicative, A](value: A)(implicit eq: Equal[A],
                                               show: Show[A]): ValidatorK[F, A] =
    Validator(
      a => Applicative[F].pure(eq.equal(value, a)),
      a => Violation.EqualTo(show.shows(value), show.shows(a)))

  def equalTo[A: Equal: Show](value: A): Validator[A] = equalToK[Id, A](value)

}
