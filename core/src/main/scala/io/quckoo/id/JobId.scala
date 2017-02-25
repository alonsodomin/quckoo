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

import upickle.default.{Reader => JsonReader, Writer => JsonWriter, _}

import io.quckoo.JobSpec

/**
  * Created by aalonsodominguez on 24/08/15.
  */
object JobId {

  /**
    * NOT SUPPORTED on ScalaJS!
    *
    * Generates the JobId related to a specific JobSpec
    *
    * @param jobSpec a job specification
    * @return a job ID
    */
  @inline def apply(jobSpec: JobSpec): JobId =
    JobId(jobSpec.jobPackage.checksum)

  @inline def apply(id: String) = new JobId(id)

  // Upickle encoders

  implicit val jobIdW: JsonWriter[JobId] = JsonWriter[JobId] { jobId =>
    implicitly[JsonWriter[String]].write(jobId.id)
  }
  implicit val jobIdR: JsonReader[JobId] = JsonReader[JobId] {
    implicitly[JsonReader[String]].read andThen JobId.apply
  }

}

final class JobId private (private val id: String) extends Serializable {

  override def equals(other: Any): Boolean = other match {
    case that: JobId => this.id == that.id
    case _           => false
  }

  override def hashCode: Int = id.hashCode()

  override def toString = id

}
