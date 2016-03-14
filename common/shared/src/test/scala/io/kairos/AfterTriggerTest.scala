package io.kairos

import io.kairos.Trigger.{LastExecutionTime, ScheduledTime, After}
import io.kairos.time.{DummyTimeSource, DummyDateTime}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 14/03/2016.
  */
class AfterTriggerTest extends WordSpec with Matchers {

  val now = new DummyDateTime(0)
  implicit val timeSource = new DummyTimeSource(now)

  "An After trigger" should {
    val delay = 10 seconds
    val trigger = After(delay)

    "never be recurring" in {
      assert(!trigger.isRecurring, "An After trigger should not be recurring")
    }

    "return a time with expected delay when has not been executed before" in {
      val expectedTime = timeSource.currentDateTime.plusMillis(delay.toMillis)
      val refTime      = ScheduledTime(timeSource.currentDateTime)

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (Some(expectedTime))
    }

    "return none as execution time if it has already been executed" in {
      val refTime = LastExecutionTime(timeSource.currentDateTime)

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (None)
    }
  }

}
