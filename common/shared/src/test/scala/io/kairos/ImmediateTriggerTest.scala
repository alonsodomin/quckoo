package io.kairos

import io.kairos.Trigger.{LastExecutionTime, ScheduledTime, Immediate}
import io.kairos.time.{DummyTimeSource, DummyDateTime}
import org.scalatest.{Matchers, WordSpec}

/**
  * Created by alonsodomin on 14/03/2016.
  */
class ImmediateTriggerTest extends WordSpec with Matchers {

  val now = new DummyDateTime(0)
  implicit val timeSource = new DummyTimeSource(now)

  "An Immediate trigger" should {
    val trigger = Immediate

    "never be recurring" in {
      assert(!trigger.isRecurring, "An immediate trigger should not be recurring")
    }

    "return now as execution time when has not been executed before" in {
      val refTime = ScheduledTime(timeSource.currentDateTime)

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (Some(timeSource.currentDateTime))
    }

    "return none as execution time if it has already been executed" in {
      val refTime = LastExecutionTime(timeSource.currentDateTime)

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (None)
    }

  }

}
