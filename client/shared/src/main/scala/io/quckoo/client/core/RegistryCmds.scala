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

package io.quckoo.client.core

import io.quckoo.JobSpec
import io.quckoo.fault._
import io.quckoo.id.JobId
import io.quckoo.protocol.registry.{JobDisabled, JobEnabled, RegisterJob}

import scalaz._

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait RegistryCmds[P <: Protocol] {
  import CmdMarshalling.Auth

  type RegisterJobCmd = Auth[P, RegisterJob, ValidationNel[Fault, JobId]]
  type GetJobCmd      = Auth[P, JobId, Option[JobSpec]]
  type GetJobsCmd     = Auth[P, Unit, Map[JobId, JobSpec]]
  type EnableJobCmd   = Auth[P, JobId, JobNotFound \/ JobEnabled]
  type DisableJobCmd  = Auth[P, JobId, JobNotFound \/ JobDisabled]

  implicit def registerJobCmd: RegisterJobCmd
  implicit def getJobCmd: GetJobCmd
  implicit def getJobsCmd: GetJobsCmd
  implicit def enableJobCmd: EnableJobCmd
  implicit def disableJobCmd: DisableJobCmd
}
