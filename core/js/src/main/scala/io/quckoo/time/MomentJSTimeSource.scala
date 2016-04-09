package io.quckoo.time

import org.widok.moment.{Date => MDate, Moment}

/**
  * Created by alonsodomin on 22/12/2015.
  */
object MomentJSTimeSource {

  def default: TimeSource = new MomentJSTimeSource(() => Moment.utc())

  def fixed(millis: Double): TimeSource = new MomentJSTimeSource(() => Moment.utc(millis))

  object Implicits {

    implicit val default = MomentJSTimeSource.default

  }

}

class MomentJSTimeSource private (moment: () => MDate) extends TimeSource {

  def currentDateTime: DateTime = new MomentJSDateTime(moment())

}
