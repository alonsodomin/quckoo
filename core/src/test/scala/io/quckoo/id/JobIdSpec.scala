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

import io.quckoo.{JobPackage, JobSpec}
import io.quckoo.serialization.json._
import io.quckoo.util.Attempt

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by alonsodomin on 26/02/2017.
  */
class JobIdSpec extends FlatSpec with Matchers {

  "JobId" should "be it's package checksum" in {
    val givenJobSpec = JobSpec("Foo", jobPackage = JobPackage.shell("bar"))

    val returnedJobId = JobId(givenJobSpec)

    returnedJobId shouldBe JobId(givenJobSpec.jobPackage.checksum)
  }

  it should "be JSON compatible" in {
    val givenJobSpec = JobSpec("Foo", jobPackage = JobPackage.shell("bar"))
    val givenJobId = JobId(givenJobSpec)

    val codec = implicitly[JsonCodec[JobId]]
    codec.encode(givenJobId).flatMap(codec.decode) shouldBe Attempt.success(givenJobId)
  }

}
