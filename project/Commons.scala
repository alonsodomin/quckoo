import org.sbtidea.SbtIdeaPlugin._
import sbt.Keys._
import sbt._

object Commons {
  val chronosVersion = "0.1.0"

  val settings: Seq[Def.Setting[_]] = Seq(
    version := chronosVersion,
    ideaExcludeFolders := ".idea" :: Nil
  )
}