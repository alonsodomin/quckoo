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

import java.time.{Clock, Instant, ZoneId, ZonedDateTime}

import io.quckoo.Trigger.{Every, LastExecutionTime, ScheduledTime}

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 14/03/2016.
  */
class EveryTriggerTest extends AnyWordSpec with Matchers {

  val instant = Instant.EPOCH
  implicit val clock = Clock.fixed(instant, ZoneId.of("UTC"))

  "with delay" should {
    val frequency = 10 seconds
    val delay     = 5 seconds
    val trigger   = Every(frequency, Some(delay))

    "always be recurring" in {
      assert(trigger.isRecurring, "An Every trigger should be recurring")
    }

    "return a time with expected delay when has not been executed before" in {
      val expectedTime = ZonedDateTime.now(clock).plusNanos(delay.toNanos)
      val refTime      = ScheduledTime(ZonedDateTime.now(clock))

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (Some(expectedTime))
    }

    "return a time with frequency delay when it has already been executed before" in {
      val expectedTime = ZonedDateTime.now(clock).plusNanos(frequency.toNanos)
      val refTime      = LastExecutionTime(ZonedDateTime.now(clock))

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (Some(expectedTime))
    }
  }

}
