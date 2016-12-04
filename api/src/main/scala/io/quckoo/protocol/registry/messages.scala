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
import io.quckoo.id.JobId
import io.quckoo.protocol.{Command, Event}

sealed trait RegistryCommand      extends Command
sealed trait RegistryReadCommand  extends RegistryCommand
sealed trait RegistryWriteCommand extends RegistryCommand

sealed trait RegistryJobCommand extends RegistryCommand {
  val jobId: JobId
}

sealed trait RegistryEvent extends Event {
  val jobId: JobId
}
sealed trait RegistryResolutionEvent extends RegistryEvent

final case class GetJob(jobId: JobId) extends RegistryReadCommand with RegistryJobCommand
case object GetJobs                   extends RegistryReadCommand

final case class RegisterJob(job: JobSpec)               extends RegistryWriteCommand
final case class JobAccepted(jobId: JobId, job: JobSpec) extends RegistryResolutionEvent
final case class JobRejected(jobId: JobId, fault: Fault) extends RegistryResolutionEvent

final case class DisableJob(jobId: JobId)  extends RegistryWriteCommand with RegistryJobCommand
final case class JobDisabled(jobId: JobId) extends RegistryEvent

final case class EnableJob(jobId: JobId) extends RegistryWriteCommand with RegistryJobCommand
final case class JobEnabled(jobId: JobId) extends RegistryEvent
