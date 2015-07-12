package io.chronos

import java.time.temporal.ChronoUnit
import java.time.{Clock, ZonedDateTime}

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 08/07/15.
 */
trait Trigger extends Serializable {

  def nextExecutionTime(clock: Clock, referenceTime: Either[ZonedDateTime, ZonedDateTime]): Option[ZonedDateTime]

  def isRecurring: Boolean = false

}

object Trigger {

  case object Immediate extends Trigger {

    override def nextExecutionTime(clock: Clock, referenceTime: Either[ZonedDateTime, ZonedDateTime]): Option[ZonedDateTime] =
      referenceTime match {
        case Left(scheduledTime)      => Some(ZonedDateTime.now(clock))
        case Right(lastExecutionTime) => None
      }

  }

  case class After(delay: FiniteDuration) extends Trigger {

    override def nextExecutionTime(clock: Clock, referenceTime: Either[ZonedDateTime, ZonedDateTime]): Option[ZonedDateTime] =
      referenceTime match {
        case Left(scheduledTime) =>
          val nanos = delay.toNanos
          Some(scheduledTime.plus(nanos, ChronoUnit.NANOS))
        case Right(lastExecutionTime) => None
      }

  }

}