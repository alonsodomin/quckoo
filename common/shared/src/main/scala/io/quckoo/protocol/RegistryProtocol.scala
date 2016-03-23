package io.quckoo.protocol

import io.quckoo.JobSpec
import io.quckoo.fault.Faults
import io.quckoo.id._

/**
 * Created by aalonsodominguez on 21/08/15.
 */
object RegistryProtocol {

  final val RegistryTopic = "Registry"

  sealed trait RegistryCommand
  sealed trait RegistryEvent

  case class GetJob(jobId: JobId) extends RegistryCommand
  case object GetJobs extends RegistryCommand

  case class RegisterJob(job: JobSpec) extends RegistryCommand
  case class JobAccepted(jobId: JobId, job: JobSpec) extends RegistryEvent
  case class JobRejected(artifactId: ArtifactId, cause: Faults) extends RegistryEvent

  case class DisableJob(jobId: JobId) extends RegistryCommand
  case class JobDisabled(jobId: JobId) extends RegistryEvent

  case class EnableJob(jobId: JobId) extends RegistryCommand
  case class JobEnabled(jobId: JobId) extends RegistryEvent

}
