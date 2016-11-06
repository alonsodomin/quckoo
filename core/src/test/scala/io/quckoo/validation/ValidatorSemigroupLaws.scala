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
  * Created by alonsodomin on 06/11/2016.
  */
trait ValidatorSemigroupLaws[F[_]] {
  implicit def A: Applicative[F]
  implicit def C: Comonad[F]
  implicit def S: Plus[ValidatorK[F, ?]]

  def associative[A: Equal](va: ValidatorK[F, A], vb: ValidatorK[F, A], vc: ValidatorK[F, A], value: A): Boolean =
    C.copure(S.plus[A](S.plus[A](va, vb), vc).run(value)) === C.copure(S.plus[A](va, S.plus[A](vb, vc)).run(value))

  def commutative[A: Equal](left: ValidatorK[F, A], right: ValidatorK[F, A], value: A): Boolean =
    C.copure(S.plus[A](left, right).run(value)) === C.copure(S.plus[A](right, left).run(value))

}

object ValidatorSemigroupLaws {
  def apply[F[_]](implicit ap: Applicative[F], co: Comonad[F], tc: Plus[ValidatorK[F, ?]]): ValidatorSemigroupLaws[F] =
    new ValidatorSemigroupLaws[F] {
      val A: Applicative[F] = ap
      val C: Comonad[F] = co
      val S: Plus[ValidatorK[F, ?]] = tc
    }
}