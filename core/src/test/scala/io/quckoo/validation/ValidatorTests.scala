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

import org.scalacheck.Arbitrary
import org.scalacheck.Prop

import org.typelevel.discipline.Laws

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 06/11/2016.
  */
trait ValidatorTests[F[_]] extends Laws {
  import Prop._

  def laws: ValidatorLaws[F]

  def rules[A : Equal : Arbitrary](semigroupName: String)(implicit arbitraryValidator: Arbitrary[ValidatorK[F, A]]): RuleSet =
    new DefaultRuleSet(
      name = semigroupName,
      parent = None,
      "leftIdentity" -> forAll(laws.leftIdentity[A] _),
      "rightIdentity" -> forAll(laws.rightIdentity[A] _),
      "commutative" -> forAll(laws.commutative[A] _),
      "associative" -> forAll(laws.associative[A] _)
    )

}

object ValidatorTests {
  def apply[F[_]: Applicative : Comonad](implicit tc: PlusEmpty[ValidatorK[F, ?]]): ValidatorTests[F] =
    new ValidatorTests[F] {
      val laws: ValidatorLaws[F] = ValidatorLaws[F]
    }
}