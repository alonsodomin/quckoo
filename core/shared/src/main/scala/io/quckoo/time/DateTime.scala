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

/**
  * Created by alonsodomin on 22/12/2015.
  */
abstract class DateTime extends Ordered[DateTime] with Serializable {
  type Repr

  def - (that: DateTime): Duration = diff(that)

  def diff(that: DateTime): Duration

  def minusMillis(millis: Long): DateTime =
    plusMillis(-millis)

  def plusMillis(millis: Long): DateTime

  def minusSeconds(seconds: Int): DateTime =
    plusSeconds(-seconds)

  def plusSeconds(seconds: Int): DateTime

  def minusMinutes(minutes: Int): DateTime =
    plusMinutes(-minutes)

  def plusMinutes(minutes: Int): DateTime

  def minusHours(hours: Int): DateTime =
    plusHours(-hours)

  def plusHours(hours: Int): DateTime

  final def compare(that: DateTime): Int = {
    if (this.toEpochSecond > that.toEpochSecond) 1
    else if (this.toEpochSecond < that.toEpochSecond) -1
    else 0
  }

  final def isBefore(dateTime: DateTime): Boolean =
    compareTo(dateTime) < 0

  final def isAfter(dateTime: DateTime): Boolean =
    compareTo(dateTime) > 0

  final def isEqual(dateTime: DateTime): Boolean =
    compareTo(dateTime) == 0

  def format(pattern: String): String

  def underlying: Repr

  def toLocal: DateTime

  def toUTC: DateTime

  def toEpochSecond: Long

  def toEpochMillis: Long

}

object DateTime {

  def now(implicit timeSource: TimeSource): DateTime =
    timeSource.currentDateTime

}