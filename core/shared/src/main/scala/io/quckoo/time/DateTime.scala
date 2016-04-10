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

  final def compare(that: DateTime): Int =
    (this.toEpochMillis - that.toEpochMillis).toInt

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

  def toEpochMillis: Long

}
