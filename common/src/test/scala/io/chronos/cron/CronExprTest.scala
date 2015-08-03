package io.chronos.cron

import org.scalatest.WordSpec

/**
 * Created by aalonsodominguez on 02/08/15.
 */
class CronExprTest extends WordSpec {

  val InvalidCronExpression = "invalidCron"
  val ValidCronExpression = "*/5 * * apr sun *"

  "isValid method" when {
    "receives an expression" should {
      "return true" in {
        assert(CronExpr.isValid(ValidCronExpression))
      }

      "return false" in {
        assert(!CronExpr.isValid(InvalidCronExpression))
      }
    }
  }

}
