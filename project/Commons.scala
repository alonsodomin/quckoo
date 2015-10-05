import org.sbtidea.SbtIdeaPlugin._
import sbt.Keys._
import sbt._

object Commons {
  val kairosVersion = "0.1.0-SNAPSHOT"

  val settings: Seq[Def.Setting[_]] = Seq(
    version := kairosVersion,
    ideaExcludeFolders := ".idea" :: Nil
  )
}