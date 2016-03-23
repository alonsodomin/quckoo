package io.quckoo

import io.quckoo.Trigger.{LastExecutionTime, ScheduledTime, At}
import io.quckoo.time.{DummyTimeSource, DummyDateTime}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 14/03/2016.
  */
class AtTriggerTest extends WordSpec with Matchers {

  "in the future" should {
    implicit val timeSource = new DummyTimeSource(new DummyDateTime(0))

    val givenTime = new DummyDateTime(10)
    val trigger   = At(givenTime)

    "never be recurring" in {
      assert(!trigger.isRecurring, "An At trigger should not be recurring")
    }

    "return the given time when has not been executed before" in {
      val refTime = timeSource.currentDateTime

      val returnedTime = trigger.nextExecutionTime(ScheduledTime(refTime))

      returnedTime should be (Some(givenTime))
    }

    "return none when it has been already executed" in {
      val refTime = timeSource.currentDateTime

      val returnedTime = trigger.nextExecutionTime(LastExecutionTime(refTime))

      returnedTime should be (None)
    }
  }

  "in the past" should {
    implicit val timeSource = new DummyTimeSource(new DummyDateTime(0))

    val givenTime = new DummyDateTime(-10)
    val trigger   = At(givenTime)

    "never be recurring" in {
      assert(!trigger.isRecurring, "An At trigger should not be recurring")
    }

    "return the given time when has not been executed before" in {
      val refTime = timeSource.currentDateTime

      val returnedTime = trigger.nextExecutionTime(ScheduledTime(refTime))

      returnedTime should be (Some(givenTime))
    }

    "return none when it has been already executed" in {
      val refTime = timeSource.currentDateTime

      val returnedTime = trigger.nextExecutionTime(LastExecutionTime(refTime))

      returnedTime should be (None)
    }
  }

  "with grace time" should {
    val givenTime = new DummyDateTime(0).plusHours(3)
    val graceTime = 2 hours
    val trigger   = At(givenTime, Some(graceTime))

    "never be recurring" in {
      assert(!trigger.isRecurring, "An At trigger should not be recurring")
    }

    "return given time when has not been executed before and now falls before any grace time" in {
      val now = new DummyDateTime(0).plusMinutes(30)
      implicit val timeSource = new DummyTimeSource(now)

      val refTime = ScheduledTime(new DummyDateTime(0))
      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (Some(givenTime))
    }

    "return now when has not been executed before and reference time falls in the grace time" in {
      val now = new DummyDateTime(0).plusHours(1)
      implicit val timeSource = new DummyTimeSource(now)

      val refTime = ScheduledTime(new DummyDateTime(0))
      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (Some(now))
    }

    "return none when has not been executed before and reference time is past outside the grace time" in {
      val now = new DummyDateTime(0).plusHours(6)
      implicit val timeSource = new DummyTimeSource(now)

      val refTime = ScheduledTime(new DummyDateTime(0))
      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (None)
    }
  }

}
