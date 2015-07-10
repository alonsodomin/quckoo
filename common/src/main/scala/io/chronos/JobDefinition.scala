package io.chronos

import io.chronos.Trigger.Immediate
import io.chronos.id.JobId

import scala.concurrent.duration.FiniteDuration

/**
 * Created by domingueza on 06/07/15.
 */

class JobDefinition(
  val jobId: JobId,
  val params: Map[String, Any] = Map.empty,
  val trigger: Trigger = Immediate,
  val triggerTimeout: Option[FiniteDuration] = None,
  val executionTimeout: Option[FiniteDuration] = None) extends Parameterizable with Serializable {

}
