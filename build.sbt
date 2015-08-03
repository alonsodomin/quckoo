
organization in ThisBuild := "io.chronos"

version in ThisBuild := Commons.chronosVersion

scalaVersion in ThisBuild := Dependencies.scalaVersion

scalacOptions in ThisBuild ++= Seq("-Xexperimental", "-language:postfixOps", "-feature")

resolvers in ThisBuild ++= Seq(
  Opts.resolver.mavenLocalFile,
  "ReactiveCouchbase Releases" at "https://raw.github.com/ReactiveCouchbase/repository/master/releases/",
  "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/"
)

lazy val chronos = (project in file(".")).aggregate(
  common, resolver, scheduler, examples, worker
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
  enablePlugins(JavaAppPackaging).
  dependsOn(common).
  dependsOn(resolver)

lazy val worker = (project in file("worker")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.workerLibs
  ).
  enablePlugins(JavaAppPackaging).
  dependsOn(common).
  dependsOn(resolver)

lazy val examples = (project in file("examples")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.examplesLibs
  ).
  dependsOn(common)