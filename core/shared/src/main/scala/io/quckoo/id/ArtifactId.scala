package io.quckoo.id

import io.quckoo.Validated
import io.quckoo.validation._
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

  implicit val instance = new Equal[ArtifactId] with Show[ArtifactId] {

    override def equal(left: ArtifactId, right: ArtifactId): Boolean =
      left.group == right.group &&
        left.artifact == right.artifact &&
        left.version == right.version

  }

}

@Lenses case class ArtifactId(group: String, artifact: String, version: String) {
  import ArtifactId._

  override def toString: String = s"$group$GroupSeparator$artifact$VersionSeparator$version"

}
