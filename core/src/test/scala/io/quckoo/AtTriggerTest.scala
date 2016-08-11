package io.quckoo

import io.quckoo.Trigger.{At, LastExecutionTime, ScheduledTime}

import org.scalatest.{Matchers, WordSpec}

import org.threeten.bp.{Clock, Instant, ZoneId, ZonedDateTime, Duration => JavaDuration}

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 14/03/2016.
  */
class AtTriggerTest extends WordSpec with Matchers {

  val instant = Instant.EPOCH
  implicit val clock = Clock.fixed(instant, ZoneId.of("UTC"))

  "in the future" should {
    val givenTime = ZonedDateTime.now(clock).plusMonths(31)
    val trigger   = At(givenTime)

    "never be recurring" in {
      assert(!trigger.isRecurring, "An At trigger should not be recurring")
    }

    "return the given time when has not been executed before" in {
      val refTime = ZonedDateTime.now(clock)

      val returnedTime = trigger.nextExecutionTime(ScheduledTime(refTime))

      returnedTime shouldBe Some(givenTime)
    }

    "return none when it has been already executed" in {
      val refTime = ZonedDateTime.now(clock)

      val returnedTime = trigger.nextExecutionTime(LastExecutionTime(refTime))

      returnedTime shouldBe None
    }
  }

  "in the past" should {
    val givenTime = ZonedDateTime.now(clock).minusMonths(22)
    val trigger   = At(givenTime)

    "never be recurring" in {
      assert(!trigger.isRecurring, "An At trigger should not be recurring")
    }

    "return the given time when has not been executed before" in {
      val refTime = ZonedDateTime.now(clock)

      val returnedTime = trigger.nextExecutionTime(ScheduledTime(refTime))

      returnedTime shouldBe Some(givenTime)
    }

    "return none when it has been already executed" in {
      val refTime = ZonedDateTime.now(clock)

      val returnedTime = trigger.nextExecutionTime(LastExecutionTime(refTime))

      returnedTime shouldBe None
    }
  }

  "with grace time" should {
    val givenTime = ZonedDateTime.now(clock).plusHours(3)
    val graceTime = 2 hours
    val trigger   = At(givenTime, Some(graceTime))

    "never be recurring" in {
      assert(!trigger.isRecurring, "An At trigger should not be recurring")
    }

    "return given time when has not been executed before and now falls before any grace time" in {
      val refTime = ScheduledTime(ZonedDateTime.now(clock))
      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime shouldBe Some(givenTime)
    }

    "return now when has not been executed before and reference time falls in the grace time" in {
      val refClock = Clock.offset(clock, JavaDuration.ofHours(1))
      val refTime = ScheduledTime(ZonedDateTime.now(clock))
      val returnedTime = trigger.nextExecutionTime(refTime)(refClock)

      returnedTime shouldBe Some(ZonedDateTime.now(refClock))
    }

    "return none when has not been executed before and reference time is past outside the grace time" in {
      val refTime = ScheduledTime(ZonedDateTime.now(clock))
      val returnedTime = trigger.nextExecutionTime(refTime)(Clock.offset(clock, JavaDuration.ofHours(6)))

      returnedTime shouldBe None
    }
  }

}
