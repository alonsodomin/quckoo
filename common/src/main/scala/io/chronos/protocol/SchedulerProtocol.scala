package io.chronos.protocol

import io.chronos.id._
import io.chronos.{Execution, JobSchedule, JobSpec}

/**
 * Created by aalonsodominguez on 08/07/15.
 */
object SchedulerProtocol {
  sealed trait Request
  sealed trait Response

  case object GetJobSpecs extends Request
  case class JobSpecs(specs: Seq[JobSpec])

  case object GetScheduledJobs extends Request
  case class ScheduledJobs(jobs: Seq[(ScheduleId, JobSchedule)]) extends Response

  case class GetExecutions(filter: Execution => Boolean) extends Request
  case class Executions(executions: Seq[Execution]) extends Response

}
