package io.chronos

import io.chronos.id._

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object protocol {

  case class JobNotRegistered(jobId: JobId)
  case class ResolutionFailed(unresolvedDependencies: Seq[String])

  type JobRejectedCause = Either[ResolutionFailed, Throwable]
  type ScheduleFailedCause = Either[JobNotRegistered, Throwable]
  type ExecutionFailedCause = Either[ResolutionFailed, Throwable]

  case class RegisterJob(job: JobSpec)
  case class JobAccepted(jobId: JobId)
  case class JobRejected(cause: JobRejectedCause)

  case class ScheduleJob(schedule: JobSchedule)
  case class ScheduleJobAck(executionId: ExecutionId)
  case class ScheduleJobFailed(cause: ScheduleFailedCause)

  case object GetRegisteredJobs
  case class RegisteredJobs(specs: Seq[JobSpec])

  case class GetJobSpec(jobId: JobId)
  case class GetSchedule(scheduleId: ScheduleId)

  // ---------- Events

  case class ExecutionEvent(executionId: ExecutionId, status: Execution.Stage)

  sealed trait WorkerEvent
  case class WorkerRegistered(workerId: WorkerId) extends WorkerEvent
  case class WorkerUnregistered(workerId: WorkerId) extends WorkerEvent
}
