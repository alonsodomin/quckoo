package io.chronos

import io.chronos.Trigger.Immediate
import io.chronos.id._

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 09/07/15.
 */
case class Schedule(jobId: JobId,
                       params: Map[String, Any] = Map.empty,
                       trigger: Trigger = Immediate,
                       timeout: Option[FiniteDuration] = None) {

  def isRecurring: Boolean = trigger.isRecurring

}
