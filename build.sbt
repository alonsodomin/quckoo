
organization in ThisBuild := "io.chronos"

scalaVersion in ThisBuild := Dependencies.scalaVersion

resolvers in ThisBuild ++= Seq(
  "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/"
)

lazy val chronos = (project in file(".")).aggregate(
  common, server, console
)

lazy val common = (project in file("common")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.commonLibs
  )

lazy val server = (project in file("server")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.serverLibs
  ).
  settings(Revolver.settings: _*).
  dependsOn(common)

lazy val console = (project in file("console")).
  settings(Commons.settings: _*).
  enablePlugins(PlayScala).
  settings(
    libraryDependencies ++= Dependencies.consoleLibs,
    routesGenerator := InjectedRoutesGenerator
  ).
  dependsOn(common)

