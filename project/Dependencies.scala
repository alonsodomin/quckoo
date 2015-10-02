import sbt._

object Dependencies {
  import Libraries._

  val scalaVersion        = "2.11.7"

  private val basicLibs: Seq[ModuleID] = Vector(
    "org.scala-lang.modules" %% "scala-xml" % "1.0.5" withSources() withJavadoc()
  ) ++ testingLibs

  val commonLibs: Seq[ModuleID] = basicLibs

  val networkLibs: Seq[ModuleID] = basicLibs ++ akkaLibs

  val resolverLibs: Seq[ModuleID] = basicLibs ++ akkaLibs ++ loggingLibs ++ Seq(
    "org.apache.ivy" % "ivy"        % "2.4.0"       withSources() withJavadoc()
  )

  val clientLibs: Seq[ModuleID] = basicLibs ++ akkaLibs ++ loggingLibs

  val clusterLibs: Seq[ModuleID] = basicLibs ++ akkaLibs ++ loggingLibs

  val schedulerLibs: Seq[ModuleID] = basicLibs ++ akkaLibs ++ loggingLibs ++ Seq(
    Akka("persistence"), Akka("cluster-sharding"),

    "com.github.krasserm" %% "akka-persistence-cassandra" % "0.4-SNAPSHOT",
    "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.1.3"  withSources() withJavadoc(),

    "commons-io"    % "commons-io" % "2.4" % "test"
  )

  val workerLibs: Seq[ModuleID] = basicLibs ++ akkaLibs ++ loggingLibs

  val exampleJobsLibs: Seq[ModuleID] = basicLibs
  val exampleProducersLibs: Seq[ModuleID] = basicLibs ++ akkaLibs

}
