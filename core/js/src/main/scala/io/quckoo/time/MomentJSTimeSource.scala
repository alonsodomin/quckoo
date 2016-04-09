package io.quckoo.time

import org.widok.moment.{Date => MDate, Moment}

/**
  * Created by alonsodomin on 22/12/2015.
  */
object MomentJSTimeSource {

  def system: TimeSource = new MomentJSTimeSource(() => Moment())

  def fixed(millis: Double): TimeSource = new MomentJSTimeSource(() => Moment(millis))

  object Implicits {

    implicit val system = MomentJSTimeSource.system

  }

}

class MomentJSTimeSource private (moment: () => MDate) extends TimeSource {

  def currentDateTime: DateTime = new MomentJSDateTime(moment())

}
