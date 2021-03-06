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

import cats._
import cats.implicits._

import org.scalatest.FunSuite
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import org.scalacheck._

import org.typelevel.discipline.scalatest.Discipline

/**
  * Created by alonsodomin on 06/11/2016.
  */
class ValidatorSpec extends FunSuite with GeneratorDrivenPropertyChecks with Discipline with ValidatorGen {
  import Arbitrary._

  {
    import conjunction._
    implicit val arbitraryIntValidator: Arbitrary[ValidatorK[Id, Int]] = arbitraryOrderValidator[Id, Int]
    implicit val arbitraryStrValidator: Arbitrary[ValidatorK[Id, String]] = Arbitrary(nonEmptyGen[Id, String])

    checkAll("Validator[Int]", ValidatorTests[Id].rules[Int]("conjunction"))
    checkAll("Validator[String]", ValidatorTests[Id].rules[String]("conjunction"))
  }

  {
    import disjunction._
    implicit val arbitraryIntValidator = arbitraryOrderValidator[Id, Int]
    implicit val arbitraryStrValidator: Arbitrary[ValidatorK[Id, String]] = Arbitrary(nonEmptyGen[Id, String])

    checkAll("Validator[Int]", ValidatorTests[Id].rules[Int]("disjunction"))
    checkAll("Validator[String]", ValidatorTests[Id].rules[String]("disjunction"))
  }

}
