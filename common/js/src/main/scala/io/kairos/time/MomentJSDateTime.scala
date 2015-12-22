package io.kairos.time

import org.widok.moment.{Units, Date}

/**
  * Created by alonsodomin on 22/12/2015.
  */
class MomentJSDateTime private[time] (private val date: Date) extends DateTime {

  def plusMillis(millis: Long): DateTime =
    new MomentJSDateTime(date.add(millis.toInt))

  def plusHours(hours: Int): DateTime =
    new MomentJSDateTime(date.add(hours, Units.Hour))

  override def equals(other: Any): Boolean = other match {
    case that: MomentJSDateTime => this.date.diff(that.date) == 0
    case _ => false
  }

  override def hashCode(): Int = date.hashCode()

}
