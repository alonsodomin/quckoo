import sbt.Keys._
import sbt._

val defaultSettings = Seq(
  organization := "io.kairos",
  version := "0.1.0-SNAPSHOT",
  resolvers += "hseeberger at bintray" at "http://dl.bintray.com/hseeberger/maven",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-deprecation",
    "-unchecked",
    "-feature",
    "-Ywarn-dead-code",
    "-language:postfixOps"
  ),
  javacOptions ++= Seq(
    "-Xlint:unchecked",
    "-Xlint:deprecation"
  ),
  javaOptions ++= Seq(
    "-Xmx2G"
  )
)

lazy val root = Project("kairos-ui-root", file(".")).
  aggregate(kairosUIJS, kairosUIJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val kairosUI = crossProject.crossType(CrossType.Full).in(file(".")).
  settings(defaultSettings: _*).settings(
  name := "kairos-ui",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "scalatags" % "0.4.6",
    "com.lihaoyi" %%% "upickle" % "0.3.6"
  )
).jsSettings(
  libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "core" % "0.9.2",
    "com.github.japgolly.scalajs-react" %%% "extra" % "0.9.2"
  ),
  jsDependencies ++= Seq(
    "org.webjars" % "react" % "0.12.2" / "react-with-addons.js" commonJSName "React"
  )
).jvmSettings(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4.0",
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.0",
    "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0",
    "com.typesafe.akka" %% "akka-http-experimental" % "1.0",
    "de.heikoseeberger" %% "akka-http-upickle" % "1.1.0"
  )
)

lazy val kairosUIJS = kairosUI.js
lazy val kairosUIJVM = kairosUI.jvm.settings(
  (resources in Compile) ++= Seq(
    (fastOptJS in (kairosUIJS, Compile)).value.data,
    (packageJSDependencies in (kairosUIJS, Compile)).value
  )
).settings(Revolver.settings: _*)
