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

import cats.{Applicative, Functor}
import cats.data.Validated

/**
  * Created by alonsodomin on 23/10/2016.
  */
trait CaseClassValidators {

  def caseClass1[F[_]: Functor, T, A](
      valid: ValidatorK[F, A]
  )(f: T => Option[A], g: A => T): ValidatorK[F, T] =
    valid.dimap[T, Validated[Violation, T]](f(_).get)(_.map(g))

  def caseClass2[F[_]: Applicative, T, A, B](
      aValid: ValidatorK[F, A],
      bValid: ValidatorK[F, B]
  )(f: T => Option[(A, B)], g: (A, B) => T): ValidatorK[F, T] =
    (aValid * bValid).dimap[T, Validated[Violation, T]](f(_).get)(_.map(g.tupled))

  def caseClass3[F[_]: Applicative, T, A, B, C](
      aValid: ValidatorK[F, A],
      bValid: ValidatorK[F, B],
      cValid: ValidatorK[F, C]
  )(f: T => Option[(A, B, C)], g: (A, B, C) => T): ValidatorK[F, T] =
    (aValid * bValid * cValid).dimap[T, Validated[Violation, T]](f(_).get)(_.map(g.tupled))

  def caseClass4[F[_]: Applicative, T, A, B, C, D](
      aValid: ValidatorK[F, A],
      bValid: ValidatorK[F, B],
      cValid: ValidatorK[F, C],
      dValid: ValidatorK[F, D]
  )(f: T => Option[(A, B, C, D)], g: (A, B, C, D) => T): ValidatorK[F, T] =
    (aValid * bValid * cValid * dValid).dimap[T, Validated[Violation, T]](f(_).get)(_.map(g.tupled))

  def caseClass5[F[_]: Applicative, T, A, B, C, D, E](
      aValid: ValidatorK[F, A],
      bValid: ValidatorK[F, B],
      cValid: ValidatorK[F, C],
      dValid: ValidatorK[F, D],
      eValid: ValidatorK[F, E]
  )(f: T => Option[(A, B, C, D, E)], g: (A, B, C, D, E) => T): ValidatorK[F, T] =
    (aValid * bValid * cValid * dValid * eValid)
      .dimap[T, Validated[Violation, T]](f(_).get)(_.map(g.tupled))

}
