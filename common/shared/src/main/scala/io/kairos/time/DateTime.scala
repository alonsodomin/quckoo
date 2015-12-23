package io.kairos.time

/**
  * Created by alonsodomin on 22/12/2015.
  */
abstract class DateTime extends Ordered[DateTime] {

  def diff(that: DateTime): Duration

  def plusMillis(millis: Long): DateTime

  def plusHours(hours: Int): DateTime

  final def compare(that: DateTime): Int =
    this compareTo that

  final def isBefore(dateTime: DateTime): Boolean =
    compareTo(dateTime) > 0

  final def isAfter(dateTime: DateTime): Boolean =
    compareTo(dateTime) < 0

  final def isEqual(dateTime: DateTime): Boolean =
    compareTo(dateTime) == 0

  def toEpochMillis: Long

}
