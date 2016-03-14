package io.kairos.time

/**
  * Created by alonsodomin on 22/12/2015.
  */
abstract class DateTime extends Ordered[DateTime] with Serializable {

  def diff(that: DateTime): Duration

  def plusMillis(millis: Long): DateTime

  def plusHours(hours: Int): DateTime

  final def compare(that: DateTime): Int =
    (this.toEpochMillis - that.toEpochMillis).toInt

  final def isBefore(dateTime: DateTime): Boolean =
    compareTo(dateTime) < 0

  final def isAfter(dateTime: DateTime): Boolean =
    compareTo(dateTime) > 0

  final def isEqual(dateTime: DateTime): Boolean =
    compareTo(dateTime) == 0

  def toUTC: DateTime

  def toEpochMillis: Long

}
