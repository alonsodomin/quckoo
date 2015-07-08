package io.chronos.protocol

import io.chronos.JobDefinition
import io.chronos.id._

/**
 * Created by aalonsodominguez on 08/07/15.
 */
object SchedulerProtocol {
  case class ScheduleJob(jobDefinition: JobDefinition)
  case class ScheduleAck(jobId: JobId)
}
