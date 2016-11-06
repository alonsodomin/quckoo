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

import io.quckoo.util.IsTraversable
import org.scalacheck._

import scalaz._

/**
  * Created by alonsodomin on 06/11/2016.
  */
trait ValidatorGen {
  import Validators._

  def greaterThanGen[F[_]: Applicative, A: Order: Show](implicit arb: Arbitrary[A]): Gen[ValidatorK[F, A]] =
    arb.arbitrary.map(greaterThanK[F, A])

  def lessThanGen[F[_]: Applicative, A: Order: Show](implicit arb: Arbitrary[A]): Gen[ValidatorK[F, A]] =
    arb.arbitrary.map(lessThanK[F, A])

  def orderGen[F[_]: Applicative, A: Order: Show: Arbitrary]: Gen[ValidatorK[F, A]] =
    Gen.oneOf(greaterThanGen[F, A], lessThanGen[F, A])

  def nonEmptyGen[F[_]: Applicative, A: IsTraversable]: Gen[ValidatorK[F, A]] =
    Gen.const(nonEmptyK[F, A])

  def arbitraryOrderValidator[F[_]: Applicative, A: Order: Show: Arbitrary]: Arbitrary[ValidatorK[F, A]] =
    Arbitrary(orderGen[F, A])

}
