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

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId, ZonedDateTime, Duration => JDuration}

/**
  * Created by alonsodomin on 22/12/2015.
  */
class JDK8DateTime(private[JDK8DateTime] val zonedDateTime: ZonedDateTime) extends DateTime {
  type Repr = ZonedDateTime

  def diff(that: DateTime): Duration =
    new JDK8Duration(JDuration.ofMillis(this.toEpochMillis - that.toEpochMillis))

  def plusMillis(millis: Long): DateTime =
    new JDK8DateTime(zonedDateTime.plus(millis, ChronoUnit.MILLIS))

  def plusSeconds(seconds: Int): DateTime =
    new JDK8DateTime(zonedDateTime.plus(seconds, ChronoUnit.SECONDS))

  def plusMinutes(minutes: Int): DateTime =
    new JDK8DateTime(zonedDateTime.plus(minutes, ChronoUnit.MINUTES))

  def plusHours(hours: Int): DateTime =
    new JDK8DateTime(zonedDateTime.plusHours(hours))

  override def equals(other: Any): Boolean = other match {
    case that: JDK8DateTime => this.zonedDateTime == that.zonedDateTime
    case _ => false
  }

  override def hashCode(): Int = zonedDateTime.hashCode()

  def underlying: ZonedDateTime = zonedDateTime

  def format(pattern: String): String =
    DateTimeFormatter.ofPattern(pattern).format(zonedDateTime)

  def toLocal: DateTime = {
    val zdt = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault())
    new JDK8DateTime(zonedDateTime = zdt)
  }

  def toUTC: DateTime = {
    val zdt = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"))
    new JDK8DateTime(zonedDateTime = zdt)
  }

  def toEpochMillis: Long =
    zonedDateTime.toInstant.toEpochMilli

  override def toString = zonedDateTime.toString

}
