package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}

import io.chronos.scheduler.JobDefinition.{Immediate, Trigger}
import io.chronos.scheduler.id.JobId

import scala.concurrent.duration.FiniteDuration

/**
 * Created by domingueza on 06/07/15.
 */
object JobDefinition {

  trait Trigger extends Serializable {

    def nextExecutionTime(clock: Clock, lastExecutionTime: Option[ZonedDateTime]): Option[ZonedDateTime]

  }

  case object Immediate extends Trigger {

    override def nextExecutionTime(clock: Clock, lastExecutionTime: Option[ZonedDateTime]): Option[ZonedDateTime] = lastExecutionTime match {
      case Some(lastExecution) => None
      case None                => Some(ZonedDateTime.now(clock))
    }

  }
  
}

case class JobDefinition(
  jobId: JobId,
  params: Map[String, Any] = Map.empty,
  jobSpec: JobSpec,
  trigger: Trigger = Immediate,
  timeout: Option[FiniteDuration] = None) extends Serializable {

}
