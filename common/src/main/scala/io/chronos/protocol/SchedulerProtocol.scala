package io.chronos.protocol

import io.chronos.id._
import io.chronos.{JobSchedule, JobSpec}

/**
 * Created by aalonsodominguez on 08/07/15.
 */
object SchedulerProtocol {
  sealed trait SchedulerRequest
  sealed trait SchedulerResponse

  case class PublishJob(job: JobSpec) extends SchedulerRequest
  
  case object GetJobSpecs extends SchedulerRequest
  case class JobSpecs(specs: Seq[JobSpec])

  case object GetScheduledJobs extends SchedulerRequest
  case class ScheduledJobs(jobs: Seq[(ScheduleId, JobSchedule)]) extends SchedulerResponse

  case class ScheduleJob(schedule: JobSchedule) extends SchedulerRequest
  case class ScheduleAck(jobId: JobId) extends SchedulerResponse
}
