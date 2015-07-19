
organization in ThisBuild := "io.chronos"

scalaVersion in ThisBuild := Dependencies.scalaVersion

scalacOptions in ThisBuild ++= Seq("-Xexperimental", "-language:postfixOps")

resolvers in ThisBuild ++= Seq(
  Opts.resolver.mavenLocalFile,
  "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",
  "ReactiveCouchbase Releases" at "https://raw.github.com/ReactiveCouchbase/repository/master/releases/",
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

lazy val resolver = (project in file("resolver")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.resolverLibs
  ).
  dependsOn(common)

lazy val scheduler = (project in file("scheduler")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.schedulerLibs
  ).
  settings(Revolver.settings: _*).
  dependsOn(common).
  dependsOn(resolver)

lazy val console = (project in file("console")).
  settings(Commons.settings: _*).
  enablePlugins(PlayScala).
  settings(
    libraryDependencies ++= Dependencies.consoleLibs,
    routesGenerator := InjectedRoutesGenerator
  ).
  dependsOn(common).
  dependsOn(examples)

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

lazy val examples = (project in file("examples")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.examplesLibs
  ).
  dependsOn(common)