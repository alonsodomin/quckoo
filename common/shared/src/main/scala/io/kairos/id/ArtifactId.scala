package io.kairos.id

import io.kairos.Validated
import io.kairos.validation.Validations
import monocle.macros.Lenses

import scalaz._

/**
 * Created by aalonsodominguez on 15/07/15.
 */
object ArtifactId {
  val GroupSeparator : Char = ':'
  val VersionSeparator : Char = '#'

  def validate(artifactId: ArtifactId): Validated[ArtifactId] =
    validate(artifactId.group, artifactId.artifact, artifactId.version)

  def validate(groupId: String, artifactId: String, version: String): Validated[ArtifactId] = {
    import Validations._

    import Scalaz._

    def validGroup: Validated[String] =
      notNullOrEmpty(groupId)("groupId")

    def validArtifactId: Validated[String] =
      notNullOrEmpty(artifactId)("artifactId")

    def validVersion: Validated[String] =
      notNullOrEmpty(version)("version")

    (validGroup |@| validArtifactId |@| validVersion) { (g, a, v) => ArtifactId(g, a, v) }
  }

}

@Lenses case class ArtifactId(group: String, artifact: String, version: String) {
  import ArtifactId._

  override def toString: String = s"$group$GroupSeparator$artifact$VersionSeparator$version"

}
