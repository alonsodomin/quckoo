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

package io.quckoo.client.http

import io.quckoo.JobSpec
import io.quckoo.client.core._
import io.quckoo.fault._
import io.quckoo.id.JobId
import io.quckoo.protocol.registry.{JobDisabled, JobEnabled, RegisterJob}
import io.quckoo.serialization.json._

import scalaz._

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait HttpRegistryCmds extends HttpMarshalling with RegistryCmds[HttpProtocol] {
  import CmdMarshalling.Auth

  private[this] def jobUrl(cmd: Command[JobId]): String = jobUrl(None)(cmd)

  private[this] def jobUrl(suffix: Option[String])(cmd: Command[JobId]): String =
    s"$JobsURI/${cmd.payload}" + suffix.map(str => s"/$str").getOrElse("")

  implicit lazy val registerJobCmd: RegisterJobCmd = new Auth[HttpProtocol, RegisterJob, ValidationNel[Fault, JobId]] {
    override val marshall = marshallToJson[RegisterJobCmd](HttpMethod.Put, _ => JobsURI)
    override val unmarshall = unmarshallFromJson[RegisterJobCmd]
  }

  implicit lazy val getJobCmd: GetJobCmd = new Auth[HttpProtocol, JobId, Option[JobSpec]] {
    override val marshall = marshallEmpty[GetJobCmd](HttpMethod.Get, jobUrl)
    override val unmarshall = unmarshalOption[JobSpec]
  }

  implicit lazy val getJobsCmd: GetJobsCmd = new Auth[HttpProtocol, Unit, Map[JobId, JobSpec]] {
    override val marshall = marshallEmpty[GetJobsCmd](HttpMethod.Get, _ => JobsURI)
    override val unmarshall = unmarshallFromJson[GetJobsCmd]
  }

  implicit lazy val enableJobCmd: EnableJobCmd = new Auth[HttpProtocol, JobId, JobNotFound \/ JobEnabled] {
    override val marshall = marshallEmpty[EnableJobCmd](HttpMethod.Post, jobUrl(Some("enable")))
    override val unmarshall = unmarshalEither[JobId, JobEnabled].map(_.leftMap(JobNotFound))
  }

  implicit lazy val disableJobCmd: DisableJobCmd = new Auth[HttpProtocol, JobId, JobNotFound \/ JobDisabled] {
    override val marshall = marshallEmpty[DisableJobCmd](HttpMethod.Post, jobUrl(Some("disable")))
    override val unmarshall = unmarshalEither[JobId, JobDisabled].map(_.leftMap(JobNotFound))
  }
}
