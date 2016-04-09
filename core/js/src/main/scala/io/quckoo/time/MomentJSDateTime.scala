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

  def underlying = date

  def toUTC: DateTime = new MomentJSDateTime(date.utc())

  def toEpochMillis: Long =
    date.milliseconds()

  override def toString: String =
    date.toISOString()

}