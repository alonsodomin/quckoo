package io.kairos.id

import io.kairos.Validated
import io.kairos.validation._
import monocle.macros.Lenses

import scalaz._

/**
 * Created by aalonsodominguez on 15/07/15.
 */
object ArtifactId {
  import Scalaz._

  val GroupSeparator : Char = ':'
  val VersionSeparator : Char = '#'

  def validate(artifactId: ArtifactId): Validated[ArtifactId] =
    validate(artifactId.group, artifactId.artifact, artifactId.version)

  def validate(groupId: String, artifactId: String, version: String): Validated[ArtifactId] = {
    import Validations._

    def validGroup: Validated[String] =
      notNullOrEmpty(groupId)("groupId")

    def validArtifactId: Validated[String] =
      notNullOrEmpty(artifactId)("artifactId")

    def validVersion: Validated[String] =
      notNullOrEmpty(version)("version")

    (validGroup |@| validArtifactId |@| validVersion)(ArtifactId.apply)
  }

}

@Lenses case class ArtifactId(group: String, artifact: String, version: String) {
  import ArtifactId._

  override def toString: String = s"$group$GroupSeparator$artifact$VersionSeparator$version"

}
