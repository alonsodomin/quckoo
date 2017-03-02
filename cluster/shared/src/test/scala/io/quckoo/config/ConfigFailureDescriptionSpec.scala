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

package io.quckoo.config

import org.scalatest.{FlatSpec, Matchers}
import pureconfig.error._

/**
  * Created by alonsodomin on 02/03/2017.
  */
class ConfigFailureDescriptionSpec extends FlatSpec with Matchers {

  "describeConfigFailures" should "return a list describing each of the config failures" in {
    val configFailures = ConfigReaderFailures(
      CannotConvertNull,
      List(
        UnknownKey("foo", None),
        CollidingKeys("quxx", "blah", Some(ConfigValueLocation("filename.conf", 10))),
        CannotConvert("bar", "Int", "invalid type", Some(ConfigValueLocation("filename.conf", 15))),
        KeyNotFound("res", None)
      )
    )

    val returnedDesc = describeConfigFailures(configFailures)
    returnedDesc shouldBe List(
      "Can not convert null value",
      "Unknow key 'foo'",
      "(filename.conf:10) :: Key 'quxx' collides in existing value: blah",
      "(filename.conf:15) :: Can not convert value 'bar' to type Int because invalid type",
      "Key 'res' not found"
    )
  }

}
