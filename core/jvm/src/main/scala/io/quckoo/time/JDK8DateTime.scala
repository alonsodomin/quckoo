package io.quckoo.time

import java.time.temporal.ChronoUnit
import java.time.{Duration => JDuration, Instant, ZoneId, ZonedDateTime}

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

  def toUTC: DateTime = {
    val zdt = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"))
    new JDK8DateTime(zonedDateTime = zdt)
  }

  def toEpochMillis: Long =
    zonedDateTime.toInstant.toEpochMilli

  override def toString = zonedDateTime.toString

}
