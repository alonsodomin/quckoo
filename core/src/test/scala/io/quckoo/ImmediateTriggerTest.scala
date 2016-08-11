package io.quckoo

import io.quckoo.Trigger.{Immediate, LastExecutionTime, ScheduledTime}

import org.scalatest.{Matchers, WordSpec}

import org.threeten.bp.{Clock, Instant, ZoneId, ZonedDateTime}

/**
  * Created by alonsodomin on 14/03/2016.
  */
class ImmediateTriggerTest extends WordSpec with Matchers {

  val instant = Instant.EPOCH
  implicit val clock = Clock.fixed(instant, ZoneId.of("UTC"))

  "An Immediate trigger" should {
    val trigger = Immediate

    "never be recurring" in {
      assert(!trigger.isRecurring, "An immediate trigger should not be recurring")
    }

    "return now as execution time when has not been executed before" in {
      val refTime = ScheduledTime(ZonedDateTime.now(clock))

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime shouldBe Some(ZonedDateTime.now(clock))
    }

    "return none as execution time if it has already been executed" in {
      val refTime = LastExecutionTime(ZonedDateTime.now(clock))

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime shouldBe None
    }

  }

}
