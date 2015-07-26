import play.sbt.PlayImport._
import sbt._

object Dependencies {
  val scalaVersion     = "2.11.7"

  val configVersion    = "1.2.1"
  val akkaVersion      = "2.3.11"
  val hazelcastVersion = "3.4.4"
  val sprayVersion     = "1.3.3"

  val log4j2Version    = "2.3"
  val slf4jVersion     = "1.7.12"

  private val basicLibs: Seq[ModuleID] = Seq(
    "org.slf4s"       %% "slf4s-api"     % slf4jVersion           withSources() withJavadoc(),
    "org.scala-lang"  %  "scala-reflect" % scalaVersion           withSources() withJavadoc(),
    "org.scalatest"   %% "scalatest"     % "2.2.4"       % "test" withSources() withJavadoc(),
    "org.slf4j"       % "slf4j-simple"   % slf4jVersion  % "test" withSources() withJavadoc()
  )

  private val akkaLibs: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor"   % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-remote"  % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-contrib" % akkaVersion          withSources() withJavadoc()
      exclude ("com.typesafe.akka", "akka-persistence-experimental_2.11"),
    "com.typesafe.akka" %% "akka-slf4j"   % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test" withSources() withJavadoc()
  )

  private val loggingLibs: Seq[ModuleID] = Seq(
    "org.apache.logging.log4j" % "log4j-api"        % log4j2Version             withSources() withJavadoc(),
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j2Version % "runtime" withSources() withJavadoc()
  )

  val commonLibs: Seq[ModuleID] = basicLibs

  val resolverLibs: Seq[ModuleID] = basicLibs ++ Seq(
    "com.typesafe"           % "config"     % configVersion withSources() withJavadoc(),
    "org.scala-lang.modules" %% "scala-xml" % "1.0.4"       withSources() withJavadoc(),
    "org.apache.ivy"         % "ivy"        % "2.4.0"       withSources() withJavadoc()
  )

  val schedulerLibs: Seq[ModuleID] = basicLibs ++ akkaLibs ++ loggingLibs ++ Seq(
    "com.hazelcast" % "hazelcast"        % hazelcastVersion withSources() withJavadoc(),
    "com.hazelcast" % "hazelcast-client" % hazelcastVersion withSources() withJavadoc(),

    "commons-io"    % "commons-io" % "2.4" % "test"
  )

  val consoleLibs: Seq[ModuleID] = basicLibs ++ akkaLibs ++ Seq(
    "org.webjars"       %% "webjars-play"      % "2.4.0-1"        withSources() withJavadoc(),
    "org.webjars"       % "font-awesome"       % "4.3.0-3",
    "org.webjars"       % "bootstrap"          % "3.3.5",
    "org.webjars"       % "knockout"           % "3.3.0",
    "org.webjars"       % "requirejs-domready" % "2.0.1-2",

    specs2 % Test  
  )

  val workerLibs: Seq[ModuleID] = basicLibs ++ akkaLibs ++ loggingLibs

  val examplesLibs: Seq[ModuleID] = basicLibs ++ akkaLibs

}