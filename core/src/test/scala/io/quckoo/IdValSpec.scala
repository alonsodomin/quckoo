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

import cats.{Eq, Show}

import io.quckoo.serialization.json.JsonCodec
import io.quckoo.util.Attempt

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by domingueza on 27/02/2017.
  */
abstract class IdValSpec[A : Eq : Show](name: String)(implicit jsonCodec: JsonCodec[A]) extends FlatSpec with Matchers {

  def generateTestId(): A

  name should "be JSON compatible" in {
    val givenId = generateTestId()

    jsonCodec.encode(givenId).flatMap(jsonCodec.decode) shouldBe Attempt.success(givenId)
  }

  it should "be equal to itself" in {
    val givenId = generateTestId()
    assert(givenId === givenId)
  }

}
