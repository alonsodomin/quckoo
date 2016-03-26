package io.quckoo.protocol.registry

import io.quckoo.JobSpec
import io.quckoo.fault._
import io.quckoo.id.{ArtifactId, JobId}

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