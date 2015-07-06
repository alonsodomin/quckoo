package io.chronos.scheduler

import io.chronos.scheduler.JobDefinition.{Immediate, Trigger}

/**
 * Created by domingueza on 06/07/15.
 */
object JobDefinition {

  trait Trigger
  case object Immediate extends Trigger
  
}

case class JobDefinition(
  jobId: String,
  params: Map[String, Any] = Map.empty,
  job: Class[_ <: Job],
  trigger: Trigger = Immediate) extends Serializable {

}
