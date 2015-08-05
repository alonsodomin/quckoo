package io.chronos.scheduler.internal

import java.time.ZonedDateTime

/**
 * Created by aalonsodominguez on 05/08/15.
 */
object Implicits {

  trait ZonedDateTimeOrdering extends Ordering[ZonedDateTime] {
    override def compare(x: ZonedDateTime, y: ZonedDateTime): Int = x.compareTo(y)
  }
  implicit object ZonedDateTime extends ZonedDateTimeOrdering

}
