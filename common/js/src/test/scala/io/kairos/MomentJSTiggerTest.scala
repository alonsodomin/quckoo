package io.kairos

import io.kairos.time.MomentJSTimeSource
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 22/12/2015.
  */
object MomentJSTiggerTest {

  val FixedMoment = 82939839020.0

}

class MomentJSTiggerTest extends WordSpec with Matchers {

  import Trigger._
  import MomentJSTiggerTest._

  implicit val timeSource = MomentJSTimeSource.fixed(FixedMoment)

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

  "An At trigger" should {
    val givenTime = timeSource.currentDateTime.plusHours(3)
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

  "An Every trigger" should {
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
