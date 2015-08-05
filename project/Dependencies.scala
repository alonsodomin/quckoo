import sbt._

object Dependencies {
  val scalaVersion        = "2.11.7"

  val configVersion       = "1.2.1"
  val akkaVersion         = "2.3.11"
  val akkaStreamsVersion  = "1.0"
  val hazelcastVersion    = "3.5.1"
  val igniteVersion       = "1.3.0-incubating"
  val sprayVersion        = "1.3.3"

  val log4j2Version       = "2.3"
  val slf4jVersion        = "1.7.12"

  private val basicLibs: Seq[ModuleID] = Vector(
    "org.slf4s"              %% "slf4s-api"                   % slf4jVersion           withSources() withJavadoc(),
    "org.scala-lang"         %  "scala-reflect"               % scalaVersion           withSources() withJavadoc(),
    "org.scala-lang.modules" %% "scala-xml"                   % "1.0.4"                withSources() withJavadoc(),
    "org.scalaz"             %% "scalaz-core"                 % "7.1.3"                withSources() withJavadoc(),
    "org.scalatest"          %% "scalatest"                   % "2.2.4"       % "test" withSources() withJavadoc(),
    "org.scalamock"          %% "scalamock-scalatest-support" % "3.2.2"       % "test" withSources() withJavadoc()
  )

  private val akkaLibs: Seq[ModuleID] = Vector(
    "com.typesafe.akka" %% "akka-actor"   % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-remote"  % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-contrib" % akkaVersion          withSources() withJavadoc()
      exclude ("com.typesafe.akka", "akka-persistence-experimental_2.11"),
    "com.typesafe.akka" %% "akka-slf4j"   % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test" withSources() withJavadoc()
  )

  private val loggingLibs: Seq[ModuleID] = Vector(
    "org.slf4j"                % "jul-to-slf4j"     % slf4jVersion  % "runtime" withSources() withJavadoc(),
    "org.apache.logging.log4j" % "log4j-api"        % log4j2Version             withSources() withJavadoc(),
    "org.apache.logging.log4j" % "log4j-core"       % log4j2Version             withSources() withJavadoc(),
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j2Version % "runtime" withSources() withJavadoc()
  )

  val commonLibs: Seq[ModuleID] = basicLibs

  val resolverLibs: Seq[ModuleID] = basicLibs ++ Seq(
    "com.typesafe"   % "config"     % configVersion withSources() withJavadoc(),
    "org.apache.ivy" % "ivy"        % "2.4.0"       withSources() withJavadoc()
  )

  val schedulerLibs: Seq[ModuleID] = basicLibs ++ akkaLibs ++ loggingLibs ++ Seq(
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamsVersion withSources() withJavadoc(),
    "com.hazelcast"      % "hazelcast"                % hazelcastVersion   withSources() withJavadoc(),
    "com.hazelcast"      % "hazelcast-client"         % hazelcastVersion   withSources() withJavadoc(),
    "com.jsuereth"      %% "scala-arm"                % "2.0.0-M1"         withSources() withJavadoc(),

    "commons-io"    % "commons-io" % "2.4" % "test"
  )

  val workerLibs: Seq[ModuleID] = basicLibs ++ akkaLibs ++ loggingLibs

  val examplesLibs: Seq[ModuleID] = basicLibs ++ akkaLibs

}