package io.chronos

import java.time.{Clock, ZonedDateTime}

/**
 * Created by aalonsodominguez on 08/07/15.
 */
trait Trigger extends Serializable {

  def nextExecutionTime(clock: Clock, lastExecutionTime: Option[ZonedDateTime]): Option[ZonedDateTime]

}

object Trigger {

  case object Immediate extends Trigger {

    override def nextExecutionTime(clock: Clock, lastExecutionTime: Option[ZonedDateTime]): Option[ZonedDateTime] = lastExecutionTime match {
      case Some(lastExecution) => None
      case None                => Some(ZonedDateTime.now(clock))
    }

  }

}