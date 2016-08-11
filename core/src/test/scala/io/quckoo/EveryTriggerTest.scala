package io.quckoo

import io.quckoo.Trigger.{Every, LastExecutionTime, ScheduledTime}

import org.scalatest.{Matchers, WordSpec}

import org.threeten.bp.{Clock, Instant, ZoneId, ZonedDateTime}

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 14/03/2016.
  */
class EveryTriggerTest extends WordSpec with Matchers {

  val instant = Instant.EPOCH
  implicit val clock = Clock.fixed(instant, ZoneId.of("UTC"))

  "with delay" should {
    val frequency = 10 seconds
    val delay     = 5 seconds
    val trigger   = Every(frequency, Some(delay))

    "always be recurring" in {
      assert(trigger.isRecurring, "An Every trigger should be recurring")
    }

    "return a time with expected delay when has not been executed before" in {
      val expectedTime = ZonedDateTime.now(clock).plusNanos(delay.toNanos)
      val refTime      = ScheduledTime(ZonedDateTime.now(clock))

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (Some(expectedTime))
    }

    "return a time with frequency delay when it has already been executed before" in {
      val expectedTime = ZonedDateTime.now(clock).plusNanos(frequency.toNanos)
      val refTime      = LastExecutionTime(ZonedDateTime.now(clock))

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (Some(expectedTime))
    }
  }

}
