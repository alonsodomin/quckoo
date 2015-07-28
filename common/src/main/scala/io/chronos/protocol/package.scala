package io.chronos

import io.chronos.id.JobId

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

  case object GetRegisteredJobs
  case class RegisteredJobs(specs: Seq[JobSpec])

  case class GetJobSpec(jobId: JobId)

}
