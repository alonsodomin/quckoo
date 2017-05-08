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

import cats.{Applicative, Id}

import io.quckoo.util.IsTraversable

/**
  * Created by alonsodomin on 21/10/2016.
  */
trait TraversableValidators {

  def nonEmptyK[F[_]: Applicative, A](implicit ev: IsTraversable[A]): ValidatorK[F, A] =
    Validator[F, A](a => Applicative[F].pure(ev.subst(a).nonEmpty), _ => Violation.Empty)

  def nonEmpty[A](implicit ev: IsTraversable[A]): Validator[A] = nonEmptyK[Id, A]

}
