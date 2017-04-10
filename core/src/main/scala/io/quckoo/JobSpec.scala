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

package io.quckoo

import cats.Show

import io.quckoo.validation._

import monocle.macros.Lenses

/**
  * Created by aalonsodominguez on 10/07/15.
  */
@Lenses final case class JobSpec(
    displayName: String,
    description: Option[String] = None,
    jobPackage: JobPackage,
    disabled: Boolean = false
)

object JobSpec {

  val valid: Validator[JobSpec] = {
    import Validators._

    val validDisplayName = nonEmpty[String].at("displayName")
    val validDetails = JobPackage.valid.at("jobPackage")

    caseClass4(validDisplayName,
      any[Option[String]],
      validDetails,
      any[Boolean])(JobSpec.unapply, JobSpec.apply)
  }

  implicit val display: Show[JobSpec] = Show.fromToString[JobSpec]

}
