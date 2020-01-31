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

package io.quckoo

import cats.implicits._

import io.quckoo.validation._

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Created by alonsodomin on 24/01/2016.
  */
class ArtifactIdSpec extends AnyFlatSpec with Matchers {
  import Violation._

  "ArtifactId" should "not accept empty strings" in {
    val expectedError =
      PathViolation(Path("organization"), Empty) and
      PathViolation(Path("name"), Empty) and
      PathViolation(Path("version"), Empty)

    ArtifactId.valid.run(ArtifactId("", "", "")) shouldBe expectedError.invalid[ArtifactId]
  }

  it should "accept any other values" in {
    val expectedArtifactId = ArtifactId("foo", "bar", "baz")
    ArtifactId.valid.run(expectedArtifactId) shouldBe expectedArtifactId.valid[Violation]
  }

}
