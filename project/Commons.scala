import org.sbtidea.SbtIdeaPlugin._
import sbt.Keys._
import sbt._

object Commons {
  val chronosVersion = "0.1.0"

  val settings: Seq[Def.Setting[_]] = Seq(
    version := chronosVersion,
    ideaExcludeFolders := ".idea" :: Nil,
    resolvers ++= Seq(
      Opts.resolver.mavenLocalFile,
      "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",
      "ReactiveCouchbase Releases" at "https://raw.github.com/ReactiveCouchbase/repository/master/releases/"
    )
  )
}