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

package io.quckoo.time

import java.time.{ZoneId, Instant, ZonedDateTime, Clock}

/**
  * Created by alonsodomin on 22/12/2015.
  */
object JDK8TimeSource {

  def default: TimeSource = new JDK8TimeSource(Clock.systemDefaultZone())

  def fixed(instant: Instant, zoneId: ZoneId): TimeSource =
    new JDK8TimeSource(Clock.fixed(instant, zoneId))

  object Implicits {

    implicit val default: TimeSource = JDK8TimeSource.default

  }

}

class JDK8TimeSource private (clock: Clock) extends TimeSource {

  def currentDateTime: DateTime = new JDK8DateTime(ZonedDateTime.now(clock))

}
