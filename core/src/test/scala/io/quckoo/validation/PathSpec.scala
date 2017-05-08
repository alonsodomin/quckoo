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

import cats._
import cats.implicits._

import org.scalacheck._

object PathSpec extends Properties("Path") {
  import Prop._
  import Arbitrary._

  val elemsGen = Gen.listOf(Gen.identifier)
  implicit lazy val elemsArbitrary: Arbitrary[List[String]] = Arbitrary(elemsGen)

  val pathGen = elemsGen.flatMap(elems => Path(elems: _*))
  implicit lazy val pathArbitrary: Arbitrary[Path] = Arbitrary(pathGen)

  property("concatenation") = forAll { (leftElems: List[String], rightElems: List[String]) =>
    Path(leftElems: _*) ++ Path(rightElems: _*) == Path((leftElems ++ rightElems): _*)
  }

  property("preseve") = forAll { path: Path =>
    Path.unapply(path.show) == Some(path)
  }

  property("elements") = forAll { elems: List[String] =>
    Path(elems: _*).elements == elems
  }

}
