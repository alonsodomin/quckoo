import sbt._
import Keys._

object Commons {
  val chronosVersion = "0.1.0"

  val settings: Seq[Def.Setting[_]] = Seq(
    version := chronosVersion,
    resolvers += Opts.resolver.mavenLocalFile
  )
}