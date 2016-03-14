package io.kairos.time

import org.widok.moment.{Moment, Units, Date}

/**
  * Created by alonsodomin on 22/12/2015.
  */
class MomentJSDateTime(private val date: Date) extends DateTime {

  def diff(that: DateTime): Duration = {
    val millis = that.toEpochMillis - this.toEpochMillis
    new MomentJSDuration(Moment.duration(millis.toInt))
  }

  def plusMillis(millis: Long): DateTime =
    new MomentJSDateTime(date.add(millis.toInt))

  def plusHours(hours: Int): DateTime =
    new MomentJSDateTime(date.add(hours, Units.Hour))

  override def equals(other: Any): Boolean = other match {
    case that: MomentJSDateTime => this.date.diff(that.date) == 0
    case _ => false
  }

  override def hashCode(): Int = date.hashCode()

  def toUTC: DateTime = new MomentJSDateTime(date.utc())

  def toEpochMillis: Long =
    date.milliseconds()

}