package io.kairos.time

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
  * Created by alonsodomin on 22/12/2015.
  */
class JDK8DateTime private[time] (private val zonedDateTime: ZonedDateTime) extends DateTime {

  def plusMillis(millis: Long): DateTime =
    new JDK8DateTime(zonedDateTime.plus(millis, ChronoUnit.MILLIS))

  def plusHours(hours: Int): DateTime =
    new JDK8DateTime(zonedDateTime.plusHours(hours))

  override def equals(other: Any): Boolean = other match {
    case that: JDK8DateTime => this.zonedDateTime == that.zonedDateTime
    case _ => false
  }

  override def hashCode(): Int = zonedDateTime.hashCode()

}
