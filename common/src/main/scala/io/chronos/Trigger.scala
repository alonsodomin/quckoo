package io.chronos

import java.time.temporal.ChronoUnit
import java.time.{Clock, ZonedDateTime}

import io.chronos.cron.CronExpr

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 08/07/15.
 */
trait Trigger extends Serializable {
  import Trigger.ReferenceTime

  def nextExecutionTime(referenceTime: ReferenceTime)(implicit clock: Clock): Option[ZonedDateTime]

  def isRecurring: Boolean = false

}

object Trigger {

  sealed trait ReferenceTime {
    val when: ZonedDateTime
  }
  case class ScheduledTime(when: ZonedDateTime) extends ReferenceTime
  case class LastExecutionTime(when: ZonedDateTime) extends ReferenceTime

  case object Immediate extends Trigger {

    override def nextExecutionTime(referenceTime: ReferenceTime)(implicit clock: Clock): Option[ZonedDateTime] = referenceTime match {
      case ScheduledTime(time)  => Some(ZonedDateTime.now(clock))
      case LastExecutionTime(_) => None
    }

  }

  case class After(delay: FiniteDuration) extends Trigger {

    override def nextExecutionTime(referenceTime: ReferenceTime)(implicit clock: Clock): Option[ZonedDateTime] =
      referenceTime match {
        case ScheduledTime(time) =>
          val nanos = delay.toNanos
          Some(time.plus(nanos, ChronoUnit.NANOS))
        case LastExecutionTime(_) => None
      }

  }

  case class Every(frequency: FiniteDuration, startingIn: Option[FiniteDuration] = None) extends Trigger {

    override def nextExecutionTime(referenceTime: ReferenceTime)(implicit clock: Clock): Option[ZonedDateTime] =
      referenceTime match {
        case ScheduledTime(time) =>
          val nanosDelay = (startingIn getOrElse 0.seconds).toNanos
          Some(time.plus(nanosDelay, ChronoUnit.NANOS))
        case LastExecutionTime(time) =>
          val nanosDelay = frequency.toNanos
          Some(time.plus(nanosDelay, ChronoUnit.NANOS))
      }

    override def isRecurring: Boolean = true

  }

  case class Cron(expr: String) extends Trigger {
    val cronExpr = CronExpr(expr)

    override def nextExecutionTime(referenceTime: ReferenceTime)(implicit clock: Clock): Option[ZonedDateTime] = {
      Some(cronExpr.next(referenceTime.when))
    }

    override def isRecurring: Boolean = true

  }

}