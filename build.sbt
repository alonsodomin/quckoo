
organization in ThisBuild := "io.chronos"

scalaVersion in ThisBuild := Dependencies.scalaVersion

resolvers in ThisBuild ++= Seq(
  "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/"
)

lazy val chronos = (project in file(".")).aggregate(
  common, scheduler, console, http, worker
)

lazy val common = (project in file("common")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.commonLibs
  )

lazy val scheduler = (project in file("scheduler")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.schedulerLibs
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

lazy val http = (project in file("http")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.httpLibs
  ).
  dependsOn(common)

lazy val worker = (project in file("worker")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.workerLibs
  ).
  dependsOn(common)
