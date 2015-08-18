
organization in ThisBuild := "io.chronos"

version in ThisBuild := Commons.chronosVersion

scalaVersion in ThisBuild := Dependencies.scalaVersion

scalacOptions in ThisBuild ++= Seq("-Xexperimental", "-language:postfixOps", "-feature")

resolvers in ThisBuild ++= Seq(
  Opts.resolver.mavenLocalFile,
  "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/",
  "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"
)

lazy val chronos = (project in file(".")).aggregate(
  common, resolver, cluster, scheduler, examples, worker
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

lazy val cluster = (project in file("cluster")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.clusterLibs
  ).
  dependsOn(common)

lazy val scheduler = (project in file("scheduler")).
  settings(Commons.settings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.schedulerLibs,
    parallelExecution in Test := false
  ).
  enablePlugins(JavaAppPackaging).
  dependsOn(common).
  dependsOn(resolver).
  dependsOn(cluster)

lazy val worker = (project in file("worker")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.workerLibs
  ).
  enablePlugins(JavaAppPackaging).
  dependsOn(common).
  dependsOn(resolver).
  dependsOn(cluster)

lazy val examples = (project in file("examples")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.examplesLibs
  ).
  dependsOn(common)