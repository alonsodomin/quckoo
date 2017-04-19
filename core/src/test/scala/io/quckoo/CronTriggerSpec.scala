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

import java.time._

import cron4s._

import io.quckoo.Trigger.{LastExecutionTime, ScheduledTime, Cron => CronTrigger}

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by alonsodomin on 03/09/2016.
  */
class CronTriggerSpec extends FlatSpec with Matchers {

  val zoneUTC = ZoneId.of("UTC")
  val instant = Instant.EPOCH
  implicit val clock = Clock.fixed(instant, zoneUTC)

  "cron triggers" should "always be recurring" in {
    val Right(cronExpr) = Cron("* * * * * ?")
    val trigger = CronTrigger(cronExpr)

    trigger.isRecurring shouldBe true
  }

  it should "return the next zoned date time to the scheduled time" in {
    val scheduledTime = ScheduledTime(LocalDateTime.of(2016, 1, 1, 0, 0, 0).atZone(zoneUTC))
    val Right(cronExpr) = Cron("0 0 * * * ?")
    val trigger = CronTrigger(cronExpr)

    val returnedTime = trigger.nextExecutionTime(scheduledTime)

    returnedTime shouldBe Some(LocalDateTime.of(2016, 1, 1, 1, 0, 0).atZone(zoneUTC))
  }

  it should "return the next zoned date time to the last execution time" in {
    val lastExecutionTime = LastExecutionTime(LocalDateTime.of(2016, 4, 2, 0, 0, 0).atZone(zoneUTC))
    val Right(cronExpr) = Cron("0 0 */2 * * ?")
    val trigger = CronTrigger(cronExpr)

    val returnedTime = trigger.nextExecutionTime(lastExecutionTime)

    returnedTime shouldBe Some(LocalDateTime.of(2016, 4, 2, 2, 0, 0).atZone(zoneUTC))
  }

}
