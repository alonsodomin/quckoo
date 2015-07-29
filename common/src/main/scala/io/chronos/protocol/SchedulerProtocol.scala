package io.chronos.protocol

import io.chronos.id._
import io.chronos.{Execution, JobSchedule, JobSpec}

/**
 * Created by aalonsodominguez on 08/07/15.
 */
object SchedulerProtocol {
  sealed trait Request
  sealed trait Response

  case class RegisterJob(job: JobSpec) extends Request
  case object RegisterJobAck extends Response
  case class RegisterJobNAck()
  
  case object GetJobSpecs extends Request
  case class JobSpecs(specs: Seq[JobSpec])

  case object GetScheduledJobs extends Request
  case class ScheduledJobs(jobs: Seq[(ScheduleId, JobSchedule)]) extends Response

  case class GetExecutions(filter: Execution => Boolean) extends Request
  case class Executions(executions: Seq[Execution]) extends Response

  case class ScheduleJob(schedule: JobSchedule) extends Request
  case class ScheduleJobAck(executionId: ExecutionId)
  case class ScheduleJobFailed(cause: ScheduleFailedCause)
  
  @deprecated
  case class ScheduleAck(jobId: JobId) extends Response

  // ----- Events -----------------

  case class ExecutionEvent(executionId: ExecutionId, status: Execution.Stage)

  sealed trait WorkerEvent

  case class WorkerRegistered(workerId: WorkerId) extends WorkerEvent
  case class WorkerUnregistered(workerId: WorkerId) extends WorkerEvent
}
