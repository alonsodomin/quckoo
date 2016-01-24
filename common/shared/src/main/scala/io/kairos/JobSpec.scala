package io.kairos

import io.kairos.id._
import io.kairos.validation.Validations
import monocle.macros.Lenses

import scalaz._

/**
 * Created by aalonsodominguez on 10/07/15.
 */
object JobSpec {

  def validate(jobSpec: JobSpec): Validated[JobSpec] =
    validate(jobSpec.displayName, jobSpec.description, jobSpec.artifactId, jobSpec.jobClass)

  def validate(displayName: String, description: String, artifactId: ArtifactId, jobClass: String): Validated[JobSpec] = {
    import Validations._

    import Scalaz._

    def validDisplayName: Validated[String] =
      notNullOrEmpty(displayName)("displayName")

    def validDescription: Validated[String] =
      notNull(description)("description")

    def validArtifactId: Validated[ArtifactId] = {
      import Validation.FlatMap._
      notNull(artifactId)("artifactId").flatMap(_ => ArtifactId.validate(artifactId))
    }

    def validJobClass: Validated[String] =
      notNullOrEmpty(jobClass)("jobClass")

    (validDisplayName |@| validDescription |@| validArtifactId |@| validJobClass) {
      (dn, desc, a, jc) => JobSpec(dn, desc, a, jc)
    }
  }

}

@Lenses case class JobSpec(
    displayName: String,
    description: String = "",
    artifactId: ArtifactId,
    jobClass: String
)
