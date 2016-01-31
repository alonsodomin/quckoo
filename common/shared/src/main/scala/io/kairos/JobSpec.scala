package io.kairos

import io.kairos.id._
import io.kairos.validation._
import monocle.macros.Lenses

import scalaz._

/**
 * Created by aalonsodominguez on 10/07/15.
 */
object JobSpec {
  import Scalaz._

  def validate(jobSpec: JobSpec): Validated[JobSpec] =
    validate(jobSpec.displayName, jobSpec.description, jobSpec.artifactId, jobSpec.jobClass)

  def validate(displayName: String, description: Option[String], artifactId: ArtifactId, jobClass: String): Validated[JobSpec] = {
    import Validations._

    def validDisplayName: Validated[String] =
      notNullOrEmpty(displayName)("displayName")

    def validDescription: Validated[Option[String]] =
      notNull(description)("description")

    def validArtifactId: Validated[ArtifactId] = {
      import Validation.FlatMap._
      notNull(artifactId)("artifactId").flatMap(_ => ArtifactId.validate(artifactId))
    }

    def validJobClass: Validated[String] =
      notNullOrEmpty(jobClass)("jobClass")

    (validDisplayName |@| validDescription |@| validArtifactId |@| validJobClass)(JobSpec.apply)
  }

}

@Lenses case class JobSpec(
    displayName: String,
    description: Option[String] = None,
    artifactId: ArtifactId,
    jobClass: String
)
