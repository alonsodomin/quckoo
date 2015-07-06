import sbt.Keys._
import sbt._

object Commons {
  val chronosVersion = "0.1.0"

  val settings: Seq[Def.Setting[_]] = Seq(
    organization := "io.chronos",
    version := chronosVersion,
    resolvers += Opts.resolver.mavenLocalFile
  )
}