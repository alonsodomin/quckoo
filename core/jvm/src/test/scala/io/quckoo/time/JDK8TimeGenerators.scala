package io.quckoo.time

import java.time.{Instant, ZoneId, ZonedDateTime}

import org.scalacheck.{Arbitrary, Gen}

/**
  * Created by alonsodomin on 10/08/2016.
  */
object JDK8TimeGenerators {

  final val ZoneUTC = ZoneId.of("UTC")

  lazy val instants: Gen[Instant] = for {
    seconds <- Gen.choose(Instant.MIN.getEpochSecond, Instant.MAX.getEpochSecond)
  } yield Instant.ofEpochSecond(seconds)
  implicit lazy val arbitraryInstant = Arbitrary(instants)

  lazy val zonedDateTimes = instants.map(_.atZone(ZoneUTC))
  implicit lazy val arbitraryZonedDateTime = Arbitrary(zonedDateTimes)

}
