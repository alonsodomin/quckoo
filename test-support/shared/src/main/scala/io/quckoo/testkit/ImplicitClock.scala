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

package io.quckoo.testkit

import org.threeten.bp._

/**
  * Created by domingueza on 27/08/15.
  */
trait ImplicitClock {

  final val FixedInstant = Instant.ofEpochMilli(893273L)
  final val ZoneUTC      = ZoneId.of("UTC")

  implicit lazy val clock = Clock.fixed(FixedInstant, ZoneUTC)

  def currentDateTime = ZonedDateTime.now(clock)

}
