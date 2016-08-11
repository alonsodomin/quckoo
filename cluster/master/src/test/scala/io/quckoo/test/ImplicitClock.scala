package io.quckoo.test

import org.threeten.bp._

/**
 * Created by domingueza on 27/08/15.
 */
trait ImplicitClock {

  final val FixedInstant = Instant.ofEpochMilli(893273L)
  final val ZoneUTC = ZoneId.of("UTC")

  implicit lazy val clock = Clock.fixed(FixedInstant, ZoneUTC)

  def currentDateTime = ZonedDateTime.now(clock)

}
