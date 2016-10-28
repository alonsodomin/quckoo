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

package io.quckoo.id

import io.quckoo.validation._

import org.scalatest.{FlatSpec, Matchers}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
class ArtifactIdTest extends FlatSpec with Matchers {
  import Violation._

  "ArtifactId" should "not accept empty strings" in {
    val expectedErrors = NonEmptyList(
      PathViolation(Path("organization"), Empty),
      PathViolation(Path("name"), Empty),
      PathViolation(Path("version"), Empty)
    ).flatMap(identity).failure[ArtifactId]
    ArtifactId.valid.run(ArtifactId("", "", "")) shouldBe expectedErrors
  }

  it should "accept any other values" in {
    val expectedArtifactId = ArtifactId("foo", "bar", "baz")
    ArtifactId.valid.run(expectedArtifactId) shouldBe expectedArtifactId.successNel[Violation]
  }

}
