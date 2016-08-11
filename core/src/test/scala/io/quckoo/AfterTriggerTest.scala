package io.quckoo

import io.quckoo.Trigger.{After, LastExecutionTime, ScheduledTime}

import org.scalatest.{Matchers, WordSpec}

import org.threeten.bp.{Clock, Instant, ZoneId, ZonedDateTime}

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 14/03/2016.
  */
class AfterTriggerTest extends WordSpec with Matchers {

  val instant = Instant.EPOCH
  implicit val clock = Clock.fixed(instant, ZoneId.of("UTC"))

  "An After trigger" should {
    val delay = 10 seconds
    val trigger = After(delay)

    "never be recurring" in {
      assert(!trigger.isRecurring, "An After trigger should not be recurring")
    }

    "return a time with expected delay when has not been executed before" in {
      val expectedTime = ZonedDateTime.now(clock).plusNanos(delay.toNanos)
      val refTime      = ScheduledTime(ZonedDateTime.now(clock))

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (Some(expectedTime))
    }

    "return none as execution time if it has already been executed" in {
      val refTime = LastExecutionTime(ZonedDateTime.now(clock))

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (None)
    }
  }

}
