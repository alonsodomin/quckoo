package io.chronos.id

/**
 * Created by aalonsodominguez on 15/07/15.
 */
object ModuleId {
  val Separator: Char = ':'
}

case class ModuleId(group: String, artifact: String, version: String) {
  import ModuleId._

  override def toString: String = s"$group$Separator$artifact$Separator$version"

}
