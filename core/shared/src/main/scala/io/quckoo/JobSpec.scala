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

  def validate(displayName: String, description: Option[String],
               artifactId: ArtifactId, jobClass: String): ValidationNel[ValidationFault, JobSpec] = {
    import Validations._

    def validDisplayName: Validation[ValidationFault, String] =
      notNullOrEmpty(displayName)("displayName")

    def validDescription: Validation[ValidationFault, Option[String]] =
      notNull(description)("description")

    def validArtifactId: ValidationNel[ValidationFault, ArtifactId] =
      (notNull(artifactId)("artifactId").toValidationNel |@| ArtifactId.validate(artifactId))((_, a) => a)

    def validJobClass: Validation[ValidationFault, String] =
      notNullOrEmpty(jobClass)("jobClass")

    (validDisplayName.toValidationNel |@| validDescription.toValidationNel
      |@| validArtifactId |@| validJobClass.toValidationNel
      |@| false.successNel[ValidationFault]
    )(JobSpec.apply)
  }

}

@Lenses case class JobSpec(
    displayName: String,
    description: Option[String] = None,
    artifactId: ArtifactId,
    jobClass: String,
    disabled: Boolean = false
)
