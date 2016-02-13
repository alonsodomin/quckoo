package io.kairos.test

import java.time.{Clock, Instant, ZoneId}

import io.kairos.time.JDK8TimeSource

/**
 * Created by domingueza on 27/08/15.
 */
trait ImplicitTimeSource {

  final val FixedInstant = Instant.ofEpochMilli(893273L)
  final val ZoneUTC = ZoneId.of("UTC")

  implicit lazy val timeSource = JDK8TimeSource.fixed(FixedInstant, ZoneUTC)

  def currentDateTime = timeSource.currentDateTime

}
