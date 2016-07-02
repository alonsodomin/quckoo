/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.protocol.registry

import io.quckoo.JobSpec
import io.quckoo.fault._
import io.quckoo.id.{ArtifactId, JobId}
import io.quckoo.protocol.{Command, Event}

import scalaz.NonEmptyList

sealed trait RegistryCommand extends Command
sealed trait RegistryReadCommand extends RegistryCommand
sealed trait RegistryWriteCommand extends RegistryCommand

sealed trait RegistryEvent extends Event {
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
