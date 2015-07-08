import Dependencies._

organization in ThisBuild := "io.chronos"

scalaVersion in ThisBuild := "2.11.6"

resolvers in ThisBuild ++= Seq(
  "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/"
)

lazy val chronos = (project in file(".")).aggregate(
  server, manager
)

lazy val server = (project in file("server")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= serverDeps
  ).
  settings(Revolver.settings: _*)

lazy val manager = (project in file("manager")).
  settings(Commons.settings: _*).
  enablePlugins(PlayScala).
  settings(
    libraryDependencies ++= managerDeps,
    routesGenerator := InjectedRoutesGenerator
  )

