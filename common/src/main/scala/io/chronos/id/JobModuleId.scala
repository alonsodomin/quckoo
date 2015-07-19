package io.chronos.id

/**
 * Created by aalonsodominguez on 15/07/15.
 */
object JobModuleId {
  val Separator: Char = ':'
}

case class JobModuleId(group: String, artifact: String, version: String, scalaVersion: Option[String] = None) {
  import JobModuleId._

  override def toString: String = s"$group$Separator$artifact$Separator$version"

}
