package io.quckoo.protocol.registry

import io.quckoo.JobSpec
import io.quckoo.fault._
import io.quckoo.id.{ArtifactId, JobId}

import scalaz.NonEmptyList

sealed trait RegistryCommand
sealed trait RegistryReadCommand extends RegistryCommand
sealed trait RegistryWriteCommand extends RegistryCommand

sealed trait RegistryEvent {
  def jobId: JobId
}
sealed trait RegistryResolutionEvent extends RegistryEvent

final case class GetJob(jobId: JobId) extends RegistryReadCommand
case object GetJobs extends RegistryReadCommand
final case class JobNotFound(jobId: JobId) extends RegistryEvent

final case class RegisterJob(job: JobSpec) extends RegistryWriteCommand
final case class JobAccepted(jobId: JobId, job: JobSpec) extends RegistryResolutionEvent with RegistryEvent
final case class JobRejected(jobId: JobId, artifactId: ArtifactId, cause: NonEmptyList[ResolutionFault]) extends RegistryResolutionEvent

final case class DisableJob(jobId: JobId) extends RegistryWriteCommand
final case class JobDisabled(jobId: JobId) extends RegistryEvent

final case class EnableJob(jobId: JobId) extends RegistryWriteCommand
final case class JobEnabled(jobId: JobId) extends RegistryEvent
