package io.quckoo.time

import org.widok.moment.{Moment, Date => MDate}
import scalajs.js

/**
  * Created by alonsodomin on 08/04/2016.
  */
class MomentJSTime(time: MDate) extends Time {
  import MomentJSTime._

  def this(tm: js.Date) =
    this(Moment.utc(tm))

  def this(hours: Int = 0, minutes: Int = 0, seconds: Int = 0, ms: Int = 0) =
    this(Moment.utc(js.Date.UTC(year = 0, month = 0, hours = hours, minutes = minutes, seconds = seconds, ms = ms)))

  def hour: Int = time.hours().toInt

  def minute: Int = time.minutes().toInt

  def seconds: Int = time.seconds().toInt

  def milliseconds: Int = time.milliseconds()

  override def toString =
    time.format(DefaultPattern)

}

object MomentJSTime {

  private final val DefaultPattern = "HH:mm:ss"

  def parse(value: String): MomentJSTime =
    new MomentJSTime(Moment.utc(value, DefaultPattern))

}
