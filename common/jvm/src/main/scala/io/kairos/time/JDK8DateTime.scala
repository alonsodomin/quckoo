package io.kairos.time

import java.time.format.DateTimeFormatter
import java.time.{Duration => JDuration, ZoneId, Instant, ZonedDateTime}
import java.time.temporal.ChronoUnit

/**
  * Created by alonsodomin on 22/12/2015.
  */
class JDK8DateTime(private[JDK8DateTime] val zonedDateTime: ZonedDateTime) extends DateTime {

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

  def toUTC: DateTime = {
    val zdt = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"))
    new JDK8DateTime(zonedDateTime = zdt)
  }

  def toEpochMillis: Long =
    zonedDateTime.toInstant.toEpochMilli

  override def toString = zonedDateTime.toString

}

object JDK8DateTime {
  import upickle.Js
  import upickle.default._

  implicit val writer: Writer[JDK8DateTime] = Writer[JDK8DateTime] {
    case dateTime =>
      val zdt = dateTime.zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"))
      Js.Str(zdt.toInstant.toString)
  }
  implicit val reader: Reader[JDK8DateTime] = Reader[JDK8DateTime] {
    case Js.Str(dateTime) =>
      val instant = Instant.parse(dateTime)
      val zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC")).
        withZoneSameInstant(ZoneId.systemDefault())
      new JDK8DateTime(zonedDateTime = zdt)
  }

}
