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

package io.quckoo.id

import upickle.default.{Reader => UReader, Writer => UWriter, _}

import io.quckoo.JobSpec

import scalaz.{Equal, Show}
import scalaz.std.string._

/**
  * Created by aalonsodominguez on 24/08/15.
  */
final class JobId private (private val id: String) extends AnyVal {

  override def toString: String = id

}

object JobId {

  /**
    * Generates the JobId related to a specific JobSpec.
    * JobSpecs with the same job package will generate the same JobId
    *
    * @param jobSpec a job specification
    * @return a job ID
    */
  @inline def apply(jobSpec: JobSpec): JobId =
    new JobId(jobSpec.jobPackage.checksum)

  @inline def apply(id: String) = new JobId(id)

  // Upickle encoders

  implicit val jobIdW: UWriter[JobId] = UWriter[JobId] { jobId =>
    implicitly[UWriter[String]].write(jobId.id)
  }
  implicit val jobIdR: UReader[JobId] = UReader[JobId] {
    implicitly[UReader[String]].read andThen JobId.apply
  }

  // Typeclass instances

  implicit val jobIdEq: Equal[JobId] = Equal.equalBy(_.id)
  implicit val jobIdShow: Show[JobId] = Show.showFromToString

}
