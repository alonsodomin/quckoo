/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.testkit.gen

import org.scalacheck._

import org.threeten.bp._
import org.threeten.bp.temporal._

/**
  * Created by alonsodomin on 05/11/2016.
  */
trait JavaTimeGenerators {
  import Arbitrary._

  val zoneIdGen: Gen[ZoneId] = Gen.oneOf(List("UTC", "GMT", "Z").map(ZoneId.of))

  implicit lazy val arbitraryZoneId = Arbitrary(zoneIdGen)

  val zoneOffsetGen: Gen[ZoneOffset] = {
    val maxSeconds = 18 * 60 * 60
    Gen.chooseNum(-maxSeconds, maxSeconds).map(ZoneOffset.ofTotalSeconds)
  }

  implicit lazy val arbitraryZoneOffset = Arbitrary(zoneOffsetGen)

  val instantGen: Gen[Instant] = for {
    seconds <- Gen.chooseNum(-31557014167219200L, 31556889864403199L)
    nanos   <- Gen.chooseNum(0, 999999999)
  } yield Instant.ofEpochSecond(seconds, nanos.toLong)

  implicit lazy val arbitraryInstant = Arbitrary(instantGen)

  val durationGen: Gen[Duration] = arbitrary[Long].map(Duration.ofNanos)

  implicit lazy val arbitraryDuration = Arbitrary(durationGen)

  val periodGen: Gen[Period] = for {
    days   <- arbitrary[Int]
    months <- arbitrary[Int]
    years  <- arbitrary[Int]
  } yield Period.of(years, months, days)

  implicit lazy val arbitraryPeriod = Arbitrary(periodGen)

  val yearGen: Gen[Year] = Gen.chooseNum(
    ChronoField.YEAR.range.getMinimum,
    ChronoField.YEAR.range.getMaximum
  ).map(value => Year.of(value.toInt))

  implicit lazy val arbitraryYear = Arbitrary(yearGen)

  val yearMonthGen: Gen[YearMonth] = for {
    year  <- arbitrary[Year]
    month <- Gen.chooseNum(1, 12)
  } yield YearMonth.of(year.getValue, month)

  implicit lazy val arbitraryYearMonth = Arbitrary(yearMonthGen)

  val localDateGen: Gen[LocalDate] = for {
    yearMonth <- arbitrary[YearMonth]
    day       <- Gen.chooseNum(1, yearMonth.lengthOfMonth)
  } yield yearMonth.atDay(day)

  implicit lazy val arbitraryLocalDate = Arbitrary(localDateGen)

  val localTimeGen: Gen[LocalTime] = for {
    hour   <- Gen.chooseNum(0, 23)
    minute <- Gen.chooseNum(0, 59)
    second <- Gen.chooseNum(0, 59)
  } yield LocalTime.of(hour, minute, second)

  implicit lazy val arbitraryLocalTime = Arbitrary(localTimeGen)

  val localDateTimeGen: Gen[LocalDateTime] = for {
    date <- arbitrary[LocalDate]
    time <- arbitrary[LocalTime]
  } yield LocalDateTime.of(date, time)

  implicit lazy val arbitraryLocalDateTime = Arbitrary(localDateTimeGen)

  val zonedDateTimeGen: Gen[ZonedDateTime] = for {
    dateTime <- arbitrary[LocalDateTime]
    zoneId   <- arbitrary[ZoneId]
  } yield dateTime.atZone(zoneId)

  implicit lazy val arbitraryZonedDateTime = Arbitrary(zonedDateTimeGen)

  val offsetDateTimeGen: Gen[OffsetDateTime] = for {
    dateTime <- arbitrary[LocalDateTime]
    offset   <- arbitrary[ZoneOffset]
  } yield dateTime.atOffset(offset)

  implicit lazy val arbitraryOffsetDateTime = Arbitrary(offsetDateTimeGen)

}
