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

trait FoldableValidators {

  def forallK[C[_], F[_]: Applicative, A](c: C[ValidatorK[F, A]])(
      implicit Fld: Foldable[C], Ap: Applicative[C], CoM: Comonad[C]
  ): ValidatorK[F, C[A]] = {
    implicit val monoid = conjunction.instance[F].monoid[C[A]]
    Fld.foldMap(c)(_.dimap(CoM.copure[A], _.map(a => Ap.pure(a))))
  }

  def forall[C[_], A](c: C[Validator[A]])(
      implicit Fld: Foldable[C], Ap: Applicative[C], CoM: Comonad[C]
  ): Validator[C[A]] =
    forallK[C, Id, A](c)

  def existsK[C[_], F[_]: Applicative, A](c: C[ValidatorK[F, A]])(
      implicit Fld: Foldable[C], Ap: Applicative[C], CoM: Comonad[C]
  ): ValidatorK[F, C[A]] = {
    implicit val monoid = disjunction.instance[F].monoid[C[A]]
    Fld.foldMap(c)(_.dimap(CoM.copure[A], _.map(a => Ap.pure(a))))
  }

  def exists[C[_], A](c: C[Validator[A]])(
      implicit Fld: Foldable[C], Ap: Applicative[C], CoM: Comonad[C]
  ): Validator[C[A]] =
    existsK[C, Id, A](c)

}
