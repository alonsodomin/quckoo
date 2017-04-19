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

package io.quckoo.serialization.json

import java.time._

import io.quckoo.serialization._
import io.quckoo.util._
import io.quckoo.testkit.gen.JavaTimeGenerators

import org.scalacheck._

/**
  * Created by alonsodomin on 05/11/2016.
  */
object JavaTimeJsonSpec extends Properties("JavaTimeJson") with JavaTimeGenerators {
  import Prop._

  private[this] def checkEncoding[A](a: A)(implicit enc: Encoder[A, String], dec: Decoder[String, A]): Boolean =
    enc.encode(a).flatMap(dec.decode) == Attempt.success(a)

  property("Instant") = forAll { (instant: Instant) =>
    checkEncoding(instant)
  }

  /*property("Duration") = forAll { (duration: Duration) =>
    checkEncoding(duration)
  }*/

  property("Period") = forAll { (period: Period) =>
    checkEncoding(period)
  }

  property("LocalDate") = forAll { (localDate: LocalDate) =>
    checkEncoding(localDate)
  }

  property("LocalTime") = forAll { (localTime: LocalTime) =>
    checkEncoding(localTime)
  }

  property("LocalDateTime") = forAll { (localDateTime: LocalDateTime) =>
    checkEncoding(localDateTime)
  }

  property("ZonedDateTime") = forAll { (zonedDateTime: ZonedDateTime) =>
    checkEncoding(zonedDateTime)
  }

  property("OffsetDateTime") = forAll { (offsetDateTime: OffsetDateTime) =>
    checkEncoding(offsetDateTime)
  }

}
