import Dependencies._

scalaVersion in ThisBuild := "2.11.6"

lazy val rootProject = Project(id = "chronos", base = file(".")).
  aggregate(chronosServer, chronosManager)

lazy val chronosServer = Project(id = "chronos-server", base = file("server")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= serverDeps
  ).
  settings(Revolver.settings: _*)

lazy val chronosManager = Project(id = "chronos-manager", base = file("manager")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= managerDeps
  )

