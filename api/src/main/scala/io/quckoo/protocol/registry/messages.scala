package io.quckoo.protocol.registry

import io.quckoo.JobSpec
import io.quckoo.fault._
import io.quckoo.id.{ArtifactId, JobId}

import scalaz.NonEmptyList

sealed trait RegistryCommand
sealed trait RegistryEvent
sealed trait RegistryResolutionEvent extends RegistryEvent

final case class GetJob(jobId: JobId) extends RegistryCommand
case object GetJobs extends RegistryCommand

final case class RegisterJob(job: JobSpec) extends RegistryCommand
final case class JobAccepted(jobId: JobId, job: JobSpec) extends RegistryResolutionEvent
final case class JobRejected(artifactId: ArtifactId, cause: NonEmptyList[Fault]) extends RegistryResolutionEvent

final case class DisableJob(jobId: JobId) extends RegistryCommand
final case class JobDisabled(jobId: JobId) extends RegistryEvent

final case class EnableJob(jobId: JobId) extends RegistryCommand
final case class JobEnabled(jobId: JobId) extends RegistryEvent