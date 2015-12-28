package io.kairos.id

/**
 * Created by aalonsodominguez on 15/07/15.
 */
object ArtifactId {
  val GroupSeparator : Char = ':'
  val VersionSeparator : Char = '#'
}

case class ArtifactId(group: String, artifact: String, version: String) {
  import ArtifactId._

  override def toString: String = s"$group$GroupSeparator$artifact$VersionSeparator$version"

}
