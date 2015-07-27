package io.chronos.protocol

import io.chronos.JobSpec
import io.chronos.id.JobId

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object RegistryProtocol {

  case class RegisterJob(job: JobSpec)
  case class JobAccepted(jobId: JobId)
  case class JobRejected(cause: JobRejectedCause)

  case object GetRegisteredJobs
  case class RegisteredJobs(specs: Seq[JobSpec])

}
