package io.chronos.id

/**
 * Created by aalonsodominguez on 15/07/15.
 */
object ModuleId {
  val GroupSeparator: Char = ':'
  val VersionSeparator: Char = '#'
}

case class ModuleId(group: String, artifact: String, version: String) {
  import ModuleId._

  override def toString: String = s"$group$GroupSeparator$artifact$VersionSeparator$version"

}
