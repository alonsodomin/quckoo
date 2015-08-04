package io.chronos

import java.time.{Clock, Instant, ZoneId, ZonedDateTime}

import org.scalatest.{Inside, Matchers, WordSpec}

import scala.concurrent.duration._

/**
 * Created by domingueza on 04/08/15.
 */
object TriggerTest {

  val FixedInstant = Instant.ofEpochMilli(82939839020L)
  val FixedZoneId  = ZoneId.systemDefault()

}

class TriggerTest extends WordSpec with Matchers with Inside {

  import Trigger._
  import TriggerTest._

  implicit val clock = Clock.fixed(FixedInstant, FixedZoneId)

  "An Immediate trigger" should {
    val trigger = Immediate

    "never be recurring" in {
      assert(!trigger.isRecurring, "An immediate trigger should not be recurring")
    }

    "return now as execution time when has not been executed before" in {
      val refTime = ScheduledTime(ZonedDateTime.now(clock))

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (Some(ZonedDateTime.now(clock)))
    }

    "return none as execution time if it has already been executed" in {
      val refTime = LastExecutionTime(ZonedDateTime.now(clock))

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (None)
    }

  }

  "An After trigger" should {
    val delay = 10.seconds
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

  "An At trigger" should {
    val givenTime = ZonedDateTime.now(clock).plusHours(3)
    val trigger   = At(givenTime)

    "never be recurring" in {
      assert(!trigger.isRecurring, "An At trigger should not be recurring")
    }

    "return the given time when has not been executed before" in {
      val refTime = ZonedDateTime.now(clock)

      val returnedTime = trigger.nextExecutionTime(ScheduledTime(refTime))

      returnedTime should be (Some(givenTime))
    }

    "return none when it has been already executed" in {
      val refTime = ZonedDateTime.now(clock)

      val returnedTime = trigger.nextExecutionTime(LastExecutionTime(refTime))

      returnedTime should be (None)
    }
  }

  "An Every trigger" should {
    val frequency = 10.seconds
    val delay     = 5.seconds
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
