/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo

import cron4s._
import cron4s.expr.CronExpr
import cron4s.lib.threetenbp._

import org.threeten.bp.{Clock, ZonedDateTime, Duration => JavaDuration}

import scala.concurrent.duration._

/**
  * Created by aalonsodominguez on 08/07/15.
  */
sealed trait Trigger {
  import Trigger.ReferenceTime

  def nextExecutionTime(referenceTime: ReferenceTime)(implicit clock: Clock): Option[ZonedDateTime]

  def isRecurring: Boolean = false

}

object Trigger {

  sealed trait ReferenceTime {
    val when: ZonedDateTime
  }
  case class ScheduledTime(when: ZonedDateTime)     extends ReferenceTime
  case class LastExecutionTime(when: ZonedDateTime) extends ReferenceTime

  case object Immediate extends Trigger {

    override def nextExecutionTime(referenceTime: ReferenceTime)(
        implicit clock: Clock): Option[ZonedDateTime] = referenceTime match {
      case ScheduledTime(_)     => Some(ZonedDateTime.now(clock))
      case LastExecutionTime(_) => None
    }

  }

  final case class After(delay: FiniteDuration) extends Trigger {

    override def nextExecutionTime(referenceTime: ReferenceTime)(
        implicit clock: Clock): Option[ZonedDateTime] =
      referenceTime match {
        case ScheduledTime(time) =>
          val nanos = delay.toNanos
          Some(time.plusNanos(nanos))

        case LastExecutionTime(_) => None
      }

  }

  final case class At(when: ZonedDateTime, graceTime: Option[FiniteDuration] = None)
      extends Trigger {

    override def nextExecutionTime(referenceTime: ReferenceTime)(
        implicit clock: Clock): Option[ZonedDateTime] =
      referenceTime match {
        case ScheduledTime(_) =>
          if (graceTime.isDefined) {
            graceTime.flatMap { margin =>
              val now  = ZonedDateTime.now(clock)
              val diff = JavaDuration.between(now, when)
              if (diff.abs.toMillis <= margin.toMillis) Some(now)
              else if (now < when) Some(when)
              else None
            }
          } else Some(when)

        case LastExecutionTime(_) => None
      }

  }

  final case class Every(frequency: FiniteDuration, startingIn: Option[FiniteDuration] = None)
      extends Trigger {

    override def nextExecutionTime(referenceTime: ReferenceTime)(
        implicit clock: Clock): Option[ZonedDateTime] =
      referenceTime match {
        case ScheduledTime(time) =>
          val delay = (startingIn getOrElse 0.seconds).toNanos
          Some(time.plusNanos(delay))

        case LastExecutionTime(time) =>
          val delay = frequency.toNanos
          Some(time.plusNanos(delay))
      }

    override val isRecurring: Boolean = true

  }

  final case class Cron(expr: CronExpr) extends Trigger {

    override def nextExecutionTime(referenceTime: ReferenceTime)(
        implicit clock: Clock): Option[ZonedDateTime] =
      expr.next(referenceTime.when)

    override val isRecurring: Boolean = true

  }

}
