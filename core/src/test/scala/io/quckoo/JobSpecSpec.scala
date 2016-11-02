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

import io.quckoo.id.ArtifactId
import io.quckoo.validation._

import org.scalatest.{FlatSpec, Matchers}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
class JobSpecSpec extends FlatSpec with Matchers {
  import Violation._

  "Validation for JobSpec parameters" should "not accept empty values" in {
    val expectedError =
      PathViolation.at(Path("displayName"), Empty) and (
        PathViolation.at(Path("artifactId", "organization"), Empty) and
        PathViolation.at(Path("artifactId", "name"), Empty) and
        PathViolation.at(Path("artifactId", "version"), Empty)
      ) and
      PathViolation.at(Path("jobClass"), Empty)

    JobSpec.valid.run(JobSpec("", None, ArtifactId("", "", ""), "")) shouldBe expectedError.failure[JobSpec]
  }

}
