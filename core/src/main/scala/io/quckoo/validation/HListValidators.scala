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
import shapeless._
import shapeless.labelled._

/**
  * Created by alonsodomin on 24/10/2016.
  */
trait HListValidators {

  def fieldValidator[K <: Symbol, F[_]: Applicative, A](validator: ValidatorK[F, A])(implicit witness: Witness.Aux[K]): ValidatorK[F, FieldType[K, A]] = {
    val labelled = validator.at(witness.value.name)
    labelled.dimap(identity, _.map(a => field[K](a)))
  }

  implicit def hnilValidator[F[_]: Applicative]: ValidatorK[F, HNil] = Validator.accept

  implicit def hlistValidator[K <: Symbol, F[_]: Applicative, H, T <: HList](
    implicit witness: Witness.Aux[K], hValidator: Lazy[ValidatorK[F, FieldType[K, H]]], tValidator: ValidatorK[F, T]): ValidatorK[F, FieldType[K, H] :: T] = {
    (hValidator.value * tValidator).dimap(hlist => (hlist.head, hlist.tail), _.map { case (h, t) => h :: t })
  }

  implicit def genericValidator[F[_]: Applicative, A, R](
    implicit gen: LabelledGeneric.Aux[A, R], validator: Lazy[ValidatorK[F, R]]): ValidatorK[F, A] = {
    validator.value.dimap(gen.to, _.map(gen.from))
  }

}
