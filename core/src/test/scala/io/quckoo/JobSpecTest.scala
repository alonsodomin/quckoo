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

import io.quckoo.fault.{Required, NotNull}
import io.quckoo.id.ArtifactId
import org.scalatest.{FlatSpec, Matchers}

import scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
class JobSpecTest extends FlatSpec with Matchers {
  import Scalaz._

  "Validation for JobSpec parameters" should "not accept nulls" in {
    val expectedErrors = NonEmptyList(
      Required("displayName"),
      NotNull("description"),
      NotNull("artifactId"),
      Required("jobClass")
    ).failure[JobSpec]
    JobSpec.validate(null, null, null, null) should be (expectedErrors)
  }

  it should "not accept empty strings in displayName or jobClass" in {
    val expectedErrors = NonEmptyList(
      Required("displayName"),
      NotNull("artifactId"),
      Required("jobClass")
    ).failure[JobSpec]
    JobSpec.validate("", None, null, "") should be (expectedErrors)
  }

  it should "not accept an invalid artifactId" in {
    val expectedErrors = NonEmptyList(
      Required("organization"),
      Required("name"),
      Required("version")
    ).failure[JobSpec]
    JobSpec.validate("foo", None, ArtifactId(null, null, null), "bar") should be (expectedErrors)
  }

}
