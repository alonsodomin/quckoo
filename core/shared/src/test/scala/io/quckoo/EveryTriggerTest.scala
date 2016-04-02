package io.quckoo

import io.quckoo.Trigger.{LastExecutionTime, ScheduledTime, Every}
import io.quckoo.time.{DummyTimeSource, DummyDateTime}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 14/03/2016.
  */
class EveryTriggerTest extends WordSpec with Matchers {

  val now = new DummyDateTime(0)
  implicit val timeSource = new DummyTimeSource(now)

  "with delay" should {
    val frequency = 10 seconds
    val delay     = 5 seconds
    val trigger   = Every(frequency, Some(delay))

    "always be recurring" in {
      assert(trigger.isRecurring, "An Every trigger should be recurring")
    }

    "return a time with expected delay when has not been executed before" in {
      val expectedTime = timeSource.currentDateTime.plusMillis(delay.toMillis)
      val refTime      = ScheduledTime(timeSource.currentDateTime)

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (Some(expectedTime))
    }

    "return a time with frequency delay when it has already been executed before" in {
      val expectedTime = timeSource.currentDateTime.plusMillis(frequency.toMillis)
      val refTime      = LastExecutionTime(timeSource.currentDateTime)

      val returnedTime = trigger.nextExecutionTime(refTime)

      returnedTime should be (Some(expectedTime))
    }
  }

}
