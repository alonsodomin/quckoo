package io.chronos

import io.chronos.Trigger.Immediate
import io.chronos.id._

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 09/07/15.
 */
class JobSchedule(val jobId: JobId,
                  val params: Map[String, Any] = Map.empty,
                  val trigger: Trigger = Immediate,
                  val triggerTimeout: Option[FiniteDuration] = None,
                  val executionTimeout: Option[FiniteDuration] = None) {

  def isRecurring: Boolean = trigger.isRecurring

}
