import play.sbt.PlayImport._
import sbt._

object Dependencies {
  val scalaVersion = "2.11.7"
  
  val akkaVersion = "2.3.11"
  val hazelcastVersion = "3.4.4"
  val sprayVersion = "1.3.3"
  val slf4jVersion = "1.7.12"

  private val sharedLibs: Seq[ModuleID] = Seq(
    "org.slf4s"       %% "slf4s-api"     % slf4jVersion,
    "org.scalatest"   %% "scalatest"     % "2.2.4"       % "test",
    "org.slf4j"       % "slf4j-simple"   % slf4jVersion  % "test"
  )

  val commonLibs: Seq[ModuleID] = sharedLibs

  val resolverLibs: Seq[ModuleID] = sharedLibs ++ Seq(
    "org.apache.ivy" % "ivy"          % "2.4.0" withSources() withJavadoc()
  )

  val schedulerLibs: Seq[ModuleID] = sharedLibs ++ Seq(
    "com.typesafe.akka" %% "akka-actor"   % akkaVersion withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-remote"  % akkaVersion withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-contrib" % akkaVersion withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-slf4j"   % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test" withSources() withJavadoc(),

    "com.hazelcast" % "hazelcast"        % hazelcastVersion withSources() withJavadoc(),
    "com.hazelcast" % "hazelcast-client" % hazelcastVersion withSources() withJavadoc(),

    "commons-io"    % "commons-io" % "2.4" % "test"
  )

  val consoleLibs: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor"   % akkaVersion withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-remote"  % akkaVersion withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion withSources() withJavadoc(),

    "org.webjars"       %% "webjars-play"      % "2.4.0-1"        withSources() withJavadoc(),
    "org.webjars"       % "font-awesome"       % "4.3.0-3",
    "org.webjars"       % "bootstrap"          % "3.3.5",
    "org.webjars"       % "knockout"           % "3.3.0",
    "org.webjars"       % "requirejs-domready" % "2.0.1-2",

    specs2 % Test  
  )

  val workerLibs: Seq[ModuleID] = sharedLibs ++ Seq(
    "com.typesafe.akka" %% "akka-actor"   % akkaVersion withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-remote"  % akkaVersion withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-slf4j"   % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test" withSources() withJavadoc()
  )

  val examplesLibs: Seq[ModuleID] = sharedLibs

}