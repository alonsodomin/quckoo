package io.kairos.time

import java.time.{ZonedDateTime, Duration => JDuration}
import java.time.temporal.ChronoUnit

/**
  * Created by alonsodomin on 22/12/2015.
  */
class JDK8DateTime private[time] (private[JDK8DateTime] val zonedDateTime: ZonedDateTime) extends DateTime {

  def diff(that: DateTime): Duration =
    new JDK8Duration(JDuration.ofMillis(that.toEpochMillis - this.toEpochMillis))

  def plusMillis(millis: Long): DateTime =
    new JDK8DateTime(zonedDateTime.plus(millis, ChronoUnit.MILLIS))

  def plusHours(hours: Int): DateTime =
    new JDK8DateTime(zonedDateTime.plusHours(hours))

  override def equals(other: Any): Boolean = other match {
    case that: JDK8DateTime => this.zonedDateTime == that.zonedDateTime
    case _ => false
  }

  override def hashCode(): Int = zonedDateTime.hashCode()

  def toEpochMillis: Long =
    zonedDateTime.toInstant.toEpochMilli

  override def toString = zonedDateTime.toString

}
