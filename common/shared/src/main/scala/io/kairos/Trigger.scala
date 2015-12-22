package io.kairos

import io.kairos.time.{DateTime, TimeSource}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 08/07/15.
 */
trait Trigger extends Serializable {
  import Trigger.ReferenceTime

  def nextExecutionTime(referenceTime: ReferenceTime)(implicit source: TimeSource): Option[DateTime]

  def isRecurring: Boolean = false

}

object Trigger {

  sealed trait ReferenceTime {
    val when: DateTime
  }
  case class ScheduledTime(when: DateTime) extends ReferenceTime
  case class LastExecutionTime(when: DateTime) extends ReferenceTime

  case object Immediate extends Trigger {

    override def nextExecutionTime(referenceTime: ReferenceTime)(implicit source: TimeSource): Option[DateTime] = referenceTime match {
      case ScheduledTime(_)     => Some(source.currentDateTime)
      case LastExecutionTime(_) => None
    }

  }

  case class After(delay: FiniteDuration) extends Trigger {

    override def nextExecutionTime(referenceTime: ReferenceTime)(implicit source: TimeSource): Option[DateTime] =
      referenceTime match {
        case ScheduledTime(time) =>
          val millis = delay.toMillis
          Some(time.plusMillis(millis))
        case LastExecutionTime(_) => None
      }

  }

  case class At(when: DateTime) extends Trigger {
    override def nextExecutionTime(referenceTime: ReferenceTime)(implicit source: TimeSource): Option[DateTime] =
      referenceTime match {
        case ScheduledTime(_)     => Some(when)
        case LastExecutionTime(_) => None
      }
  }

  case class Every(frequency: FiniteDuration, startingIn: Option[FiniteDuration] = None) extends Trigger {

    override def nextExecutionTime(referenceTime: ReferenceTime)(implicit source: TimeSource): Option[DateTime] =
      referenceTime match {
        case ScheduledTime(time) =>
          val millisDelay = (startingIn getOrElse 0.seconds).toMillis
          Some(time.plusMillis(millisDelay))
        case LastExecutionTime(time) =>
          val millisDelay = frequency.toMillis
          Some(time.plusMillis(millisDelay))
      }

    override def isRecurring: Boolean = true

  }

}