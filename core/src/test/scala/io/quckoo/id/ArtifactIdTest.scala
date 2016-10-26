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

import io.quckoo.fault.{Required, Fault}
import org.scalatest.{FlatSpec, Matchers}

import scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
class ArtifactIdTest extends FlatSpec with Matchers {
  import Scalaz._

  "Validation for ArtifactId parameters" should "not accept nulls" in {
    val expectedErrors = NonEmptyList(
        Required("organization"),
        Required("name"),
        Required("version")
    ).failure[ArtifactId]
    ArtifactId.validate(null, null, null) should be (expectedErrors)
  }

  it should "not accept empty strings" in {
    val expectedErrors = NonEmptyList(
      Required("organization"),
      Required("name"),
      Required("version")
    ).failure[ArtifactId]
    ArtifactId.validate("", "", "") should be (expectedErrors)
  }

  it should "not accept any combination of nulls or empty strings" in {
    val expectedErrors = NonEmptyList(
      Required("organization"),
      Required("version")
    ).failure[ArtifactId]
    ArtifactId.validate(null, "foo", "") should be (expectedErrors)
  }

  it should "accept any other values" in {
    val expectedArtifactId = ArtifactId("foo", "bar", "baz")
    ArtifactId.validate(expectedArtifactId) should be (expectedArtifactId.successNel[Fault])
  }

}
