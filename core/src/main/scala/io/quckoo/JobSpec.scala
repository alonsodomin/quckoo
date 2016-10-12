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

import io.quckoo.fault._
import io.quckoo.id._
import io.quckoo.validation._
import monocle.macros.Lenses

import scalaz._

/**
  * Created by aalonsodominguez on 10/07/15.
  */
object JobSpec {
  import Scalaz._

  def validate(jobSpec: JobSpec): ValidationNel[ValidationFault, JobSpec] =
    validate(jobSpec.displayName, jobSpec.description, jobSpec.artifactId, jobSpec.jobClass)

  def validate(displayName: String,
               description: Option[String],
               artifactId: ArtifactId,
               jobClass: String): ValidationNel[ValidationFault, JobSpec] = {
    import Validations._

    def validDisplayName: Validation[ValidationFault, String] =
      notNullOrEmpty(displayName)("displayName")

    def validDescription: Validation[ValidationFault, Option[String]] =
      notNull(description)("description")

    def validArtifactId: ValidationNel[ValidationFault, ArtifactId] = {
      import Validation.FlatMap._
      val nullcheck = notNull(artifactId)("artifactId").toValidationNel
      nullcheck.flatMap(_ => ArtifactId.validate(artifactId))
    }

    def validJobClass: Validation[ValidationFault, String] =
      notNullOrEmpty(jobClass)("jobClass")

    (validDisplayName.toValidationNel |@| validDescription.toValidationNel
      |@| validArtifactId |@| validJobClass.toValidationNel
      |@| false.successNel[ValidationFault])(JobSpec.apply)
  }

}

@Lenses case class JobSpec(
    displayName: String,
    description: Option[String] = None,
    artifactId: ArtifactId,
    jobClass: String,
    disabled: Boolean = false
)
