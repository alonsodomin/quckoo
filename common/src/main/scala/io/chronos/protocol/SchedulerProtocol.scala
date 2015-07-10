package io.chronos.protocol

import io.chronos.JobDefinition
import io.chronos.id._

import scala.concurrent.Promise

/**
 * Created by aalonsodominguez on 08/07/15.
 */
object SchedulerProtocol {
  sealed trait SchedulerRequest
  sealed trait SchedulerResponse

  case class GetScheduledJobs(jobs: Promise[Seq[JobDefinition]]) extends SchedulerRequest
  case class ScheduledJobs(jobs: Seq[JobDefinition]) extends SchedulerResponse

  case class ScheduleJob(jobDefinition: JobDefinition) extends SchedulerRequest
  case class ScheduleAck(jobId: JobId) extends SchedulerResponse
}
