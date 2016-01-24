package io.kairos.id

import monocle.macros.Lenses

/**
 * Created by aalonsodominguez on 15/07/15.
 */
object ArtifactId {
  val GroupSeparator : Char = ':'
  val VersionSeparator : Char = '#'
}

@Lenses case class ArtifactId(group: String, artifact: String, version: String) {
  import ArtifactId._

  override def toString: String = s"$group$GroupSeparator$artifact$VersionSeparator$version"

}
