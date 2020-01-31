/*
 * Copyright 2015 A. Alonso Dominguez
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

package io.quckoo

import cats.{Eq, Show}
import cats.instances.string._

import io.circe.{Encoder, Decoder, KeyDecoder, KeyEncoder, Codec}

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

  // Circe encoding/decoding

  implicit val jobIdJsonCodec: Codec[JobId] =
    Codec.from(Decoder[String].map(apply), Encoder[String].contramap(_.id))

  implicit val jobIdKeyEncoder: KeyEncoder[JobId] = KeyEncoder.instance(_.id)

  implicit val jobIdKeyDecoder: KeyDecoder[JobId] = KeyDecoder.instance(id => Some(apply(id)))

  // Typeclass instances

  implicit val jobIdEq: Eq[JobId]     = Eq.by(_.id)
  implicit val jobIdShow: Show[JobId] = Show.fromToString

}
