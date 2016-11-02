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

package io.quckoo

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 21/10/2016.
  */
package object validation extends ValidatorSyntax {
  type ValidatorK[F[_], A] = Kleisli[F, A, Validation[Violation, A]]
  type Validator[A] = ValidatorK[Id, A]

  object Validator {
    def apply[F[_]: Functor, A](test: A => F[Boolean], err: A => Violation): ValidatorK[F, A] = Kleisli { a =>
      test(a).map(cond => if (cond) a.success[Violation] else err(a).failure[A])
    }

    def accept[F[_], A](implicit ev: Applicative[F]): ValidatorK[F, A] =
      Kleisli { a => ev.pure(a.success[Violation]) }
  }

}
