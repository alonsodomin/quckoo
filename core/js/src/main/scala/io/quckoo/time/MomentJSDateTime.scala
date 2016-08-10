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

import org.widok.moment.{Moment, Units, Date => MDate}
import scalajs.js

/**
  * Created by alonsodomin on 22/12/2015.
  */
class MomentJSDateTime(private val date: MDate) extends DateTime {
  type Repr = MDate

  def this(date: Date, time: Time) =
    this(Moment.utc(new js.Date(date.year, date.month, date.dayOfMonth,
      time.hour, time.minute, time.seconds, time.milliseconds))
    )

  def diff(that: DateTime): Duration = {
    val millis = this.toEpochMillis - that.toEpochMillis
    new MomentJSDuration(Moment.duration(millis.toInt))
  }

  def plusMillis(millis: Long): DateTime =
    new MomentJSDateTime(date.add(millis.toInt))

  def plusSeconds(seconds: Int): DateTime =
    new MomentJSDateTime(date.add(seconds, Units.Second))

  def plusMinutes(minutes: Int): DateTime =
    new MomentJSDateTime(date.add(minutes, Units.Minute))

  def plusHours(hours: Int): DateTime =
    new MomentJSDateTime(date.add(hours, Units.Hour))

  override def equals(other: Any): Boolean = other match {
    case that: MomentJSDateTime => this.date.diff(that.date) == 0
    case _ => false
  }

  override def hashCode(): Int = date.hashCode()

  def format(pattern: String): String =
    date.format(pattern)

  def underlying = date

  def toLocal: DateTime = new MomentJSDateTime(date.local())

  def toUTC: DateTime = new MomentJSDateTime(date.utc())

  def toEpochSecond: Long =
    date.second()

  def toEpochMillis: Long =
    date.millisecond()

  override def toString: String =
    date.toISOString()

}
