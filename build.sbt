import play.Project._
import Dependencies._

scalaVersion in ThisBuild := "2.11.6"

lazy val chronosServer = Project(id = "chronos-server", base = file("server")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= serverDeps
  )

lazy val chronosManager = Project(id = "chronos-manager", base = file("manager")).
  settings(Commons.settings: _*).
  settings(playScalaSettings: _*).
  settings(
    libraryDependencies ++= managerDeps
  )

