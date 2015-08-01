package io.chronos

import io.chronos.id._

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object protocol {

  case class GetJob(jobId: JobId)
  case object GetJobs

  case class GetSchedule(scheduleId: ScheduleId)
  case object GetScheduledJobs

  case class GetExecution(executionId: ExecutionId)
  case class GetExecutions(filter: Execution => Boolean)

  // --------- Commands

  case class JobNotRegistered(jobId: JobId)
  case class ResolutionFailed(unresolvedDependencies: Seq[String])

  type JobRejectedCause = Either[ResolutionFailed, Throwable]
  type ScheduleFailedCause = Either[JobNotRegistered, Throwable]
  type ExecutionFailedCause = Either[ResolutionFailed, Throwable]

  case class RegisterJob(job: JobSpec)
  case class JobAccepted(jobId: JobId)
  case class JobRejected(cause: JobRejectedCause)

  case class ScheduleJob(schedule: JobSchedule)
  case class RescheduleJob(scheduleId: ScheduleId)
  case class ScheduleJobAck(executionId: ExecutionId)
  case class ScheduleJobFailed(cause: ScheduleFailedCause)

  // ---------- Events

  case class ExecutionEvent(executionId: ExecutionId, status: Execution.Stage)

  sealed trait WorkerEvent
  case class WorkerRegistered(workerId: WorkerId) extends WorkerEvent
  case class WorkerUnregistered(workerId: WorkerId) extends WorkerEvent
}
