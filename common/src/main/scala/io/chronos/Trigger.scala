package io.chronos

import java.time.temporal.ChronoUnit
import java.time.{Clock, ZonedDateTime}

import scala.concurrent.duration.TimeUnit

/**
 * Created by aalonsodominguez on 08/07/15.
 */
trait Trigger extends Serializable {

  def nextExecutionTime(clock: Clock, lastExecutionTime: Option[ZonedDateTime]): Option[ZonedDateTime]

  def isRecurring: Boolean = false

}

object Trigger {

  case object Immediate extends Trigger {

    override def nextExecutionTime(clock: Clock, lastExecutionTime: Option[ZonedDateTime]): Option[ZonedDateTime] = lastExecutionTime match {
      case Some(lastExecution) => None
      case None                => Some(ZonedDateTime.now(clock))
    }

  }

  case class Delay(amount: Long, unit: TimeUnit) extends Trigger {

    override def nextExecutionTime(clock: Clock, lastExecutionTime: Option[ZonedDateTime]): Option[ZonedDateTime] = lastExecutionTime match {
      case Some(lastExecution) => None
      case None                =>
        val now   = ZonedDateTime.now(clock)
        val nanos = unit.toNanos(amount)
        Some(now.plus(nanos, ChronoUnit.NANOS))
    }

  }

}