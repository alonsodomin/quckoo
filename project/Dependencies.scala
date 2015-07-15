import play.sbt.PlayImport._
import sbt._

object Dependencies {
  val scalaVersion = "2.11.7"
  
  val akkaVersion = "2.3.11"
  val hazelcastVersion = "3.4.4"
  val sprayVersion = "1.3.3"

  val commonLibs: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor"   % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-remote"  % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-contrib" % akkaVersion          withSources() withJavadoc(),

    "org.scala-lang"  % "scala-compiler" % scalaVersion,
    "org.scalatest"   %% "scalatest"     % "2.2.4" % "test"
  )

  val schedulerLibs: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-slf4j"   % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test" withSources() withJavadoc(),

    "com.hazelcast" % "hazelcast"        % hazelcastVersion withSources() withJavadoc(),
    "com.hazelcast" % "hazelcast-client" % hazelcastVersion withSources() withJavadoc(),

    "org.reactivecouchbase" %% "reactivecouchbase-core" % "0.3" withSources() withJavadoc(),

    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "commons-io"    % "commons-io" % "2.4" % "test"
  )

  val consoleLibs: Seq[ModuleID] = Seq(
    "com.hazelcast"     % "hazelcast-client"   % hazelcastVersion withSources() withJavadoc(),

    "org.webjars"       %% "webjars-play"      % "2.4.0-1"        withSources() withJavadoc(),
    "org.webjars"       % "font-awesome"       % "4.3.0-3",
    "org.webjars"       % "bootstrap"          % "3.3.5",
    "org.webjars"       % "knockout"           % "3.3.0",
    "org.webjars"       % "requirejs-domready" % "2.0.1-2",

    specs2 % Test  
  )

  val httpLibs: Seq[ModuleID] = Seq(
    "io.spray" %% "spray-can"     % sprayVersion          withSources() withJavadoc(),
    "io.spray" %% "spray-routing" % sprayVersion          withSources() withJavadoc(),
    "io.spray" %% "spray-httpx"   % sprayVersion          withSources() withJavadoc(),
    "io.spray" %% "spray-testkit" % sprayVersion % "test" withSources() withJavadoc(),

    "org.scalatest" %% "scalatest" % "2.2.4" % "test"
  )

  val workerLibs: Seq[ModuleID] = Seq(
    "org.apache.ivy"     % "ivy"    % "2.4.0",
    "org.eclipse.aether" % "aether" % "1.0.2.v20150114"
  )

  val examplesLibs: Seq[ModuleID] = Seq(
  )

}