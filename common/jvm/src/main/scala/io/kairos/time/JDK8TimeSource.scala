package io.kairos.time

import java.time.{ZoneId, Instant, ZonedDateTime, Clock}

/**
  * Created by alonsodomin on 22/12/2015.
  */
object JDK8TimeSource {

  def default: TimeSource = new JDK8TimeSource(Clock.systemDefaultZone())

  def fixed(instant: Instant, zoneId: ZoneId): TimeSource =
    new JDK8TimeSource(Clock.fixed(instant, zoneId))

  object Implicits {

    implicit lazy val default: TimeSource = JDK8TimeSource.default

  }

}

class JDK8TimeSource private (clock: Clock) extends TimeSource {

  def currentDateTime: DateTime = new JDK8DateTime(ZonedDateTime.now(clock))

}
