package io.quckoo

import cron4s._

import io.quckoo.Trigger.{LastExecutionTime, ScheduledTime, Cron => CronTrigger}

import org.threeten.bp._

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by alonsodomin on 03/09/2016.
  */
class CronTriggerSpec extends FlatSpec with Matchers {

  val zoneUTC = ZoneId.of("UTC")
  val instant = Instant.EPOCH
  implicit val clock = Clock.fixed(instant, zoneUTC)

  "cron triggers" should "always be recurring" in {
    val Right(cronExpr) = Cron("* * * * * *")
    val trigger = CronTrigger(cronExpr)

    trigger.isRecurring shouldBe true
  }

  it should "return the next zoned date time to the scheduled time" in {
    val scheduledTime = ScheduledTime(LocalDateTime.of(2016, 1, 1, 0, 0, 0).atZone(zoneUTC))
    val Right(cronExpr) = Cron("0 0 * * * *")
    val trigger = CronTrigger(cronExpr)

    val returnedTime = trigger.nextExecutionTime(scheduledTime)

    returnedTime shouldBe Some(LocalDateTime.of(2016, 1, 1, 1, 0, 0).atZone(zoneUTC))
  }

  it should "return the next zoned date time to the last execution time" in {
    val lastExecutionTime = LastExecutionTime(LocalDateTime.of(2016, 4, 2, 0, 0, 0).atZone(zoneUTC))
    val Right(cronExpr) = Cron("0 0 */2 * * *")
    val trigger = CronTrigger(cronExpr)

    val returnedTime = trigger.nextExecutionTime(lastExecutionTime)

    returnedTime shouldBe Some(LocalDateTime.of(2016, 4, 2, 2, 0, 0).atZone(zoneUTC))
  }

}
