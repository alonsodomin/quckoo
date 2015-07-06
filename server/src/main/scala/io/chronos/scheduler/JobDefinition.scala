package io.chronos.scheduler

import io.chronos.scheduler.JobDefinition.{Execution, Immediate, Trigger}

/**
 * Created by domingueza on 06/07/15.
 */
object JobDefinition {
  type Execution = () => Any

  trait Trigger
  case object Immediate extends Trigger
}

case class JobDefinition(
  jobId: String,
  params: Map[String, Any] = Map.empty,
  execution: Execution,
  trigger: Trigger = Immediate) {

}
