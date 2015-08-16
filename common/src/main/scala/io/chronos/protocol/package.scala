package io.chronos

import io.chronos.Trigger.Immediate
import io.chronos.id._

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object protocol {

  case class GetJob(jobId: JobId)
  case object GetJobs
  
  // --------- Commands

  case class JobNotEnabled(jobId: JobId)
  case class ResolutionFailed(unresolvedDependencies: Seq[String])

  type JobRejectedCause = Either[ResolutionFailed, Throwable]
  type ScheduleFailedCause = Either[JobNotEnabled, Throwable]
  type ExecutionFailedCause = Either[ResolutionFailed, Throwable]

  sealed trait RegistryCommand
  sealed trait RegistryEvent
  case class RegisterJob(job: JobSpec) extends RegistryCommand
  case class JobAccepted(jobId: JobId, job: JobSpec) extends RegistryEvent
  case class JobRejected(cause: JobRejectedCause) extends RegistryEvent

  case class DisableJob(jobId: JobId) extends RegistryCommand
  case class JobDisabled(jobId: JobId) extends RegistryEvent
  
  case class ScheduleJob(jobId: JobId,
                         params: Map[String, AnyVal] = Map.empty,
                         trigger: Trigger = Immediate,
                         timeout: Option[FiniteDuration] = None)

}
