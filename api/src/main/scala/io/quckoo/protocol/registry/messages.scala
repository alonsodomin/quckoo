package io.quckoo.protocol.registry

import io.quckoo.JobSpec
import io.quckoo.fault._
import io.quckoo.id.{ArtifactId, JobId}

import scalaz.NonEmptyList

sealed trait RegistryCommand
sealed trait RegistryEvent
sealed trait RegistryResolutionEvent extends RegistryEvent

case class GetJob(jobId: JobId) extends RegistryCommand
case object GetJobs extends RegistryCommand

case class RegisterJob(job: JobSpec) extends RegistryCommand
case class JobAccepted(jobId: JobId, job: JobSpec) extends RegistryResolutionEvent
case class JobRejected(artifactId: ArtifactId, cause: NonEmptyList[Fault]) extends RegistryResolutionEvent

case class DisableJob(jobId: JobId) extends RegistryCommand
case class JobDisabled(jobId: JobId) extends RegistryEvent

case class EnableJob(jobId: JobId) extends RegistryCommand
case class JobEnabled(jobId: JobId) extends RegistryEvent