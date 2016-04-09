package io.quckoo.time

import org.widok.moment.{Moment, Date => MDate}
import scalajs.js

/**
  * Created by alonsodomin on 08/04/2016.
  */
class MomentJSDate(date: MDate) extends Date {
  import MomentJSDate._

  def this(year: Int, month: Int, day: Int) =
    this(Moment.utc(new js.Date(year, month, day)))

  def this(dt: js.Date) =
    this(Moment.utc(dt))

  def year: Int = date.years().toInt

  def month: Int = date.months().toInt

  def dayOfMonth: Int = date.days().toInt

  def format(pattern: String) =
    date.format(pattern)

  override def toString =
    date.format(DefaultPattern)

}

object MomentJSDate {

  private final val DefaultPattern = "YYYY-MM-DD"

  def parse(value: String): MomentJSDate =
    new MomentJSDate(Moment.utc(value, DefaultPattern))

}
