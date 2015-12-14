import sbt._
import Keys._

lazy val commonSettings = Seq(
  organization := "io.kairos",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq(
    "-Xexperimental",
    "-language:postfixOps",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Ywarn-dead-code"
  ),
  resolvers ++= Seq(
    Opts.resolver.mavenLocalFile,
    "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven",
    "hseeberger at bintray" at "http://dl.bintray.com/hseeberger/maven",
    "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"
  )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val commonJsSettins = Seq(
  scalaJSStage in Global := FastOptStage
)

lazy val scoverageSettings = Seq(
  coverageHighlighting := true
)

lazy val kairos = (project in file(".")).aggregate(
  common, network, client, cluster, kernel, consoleRoot, examples, worker
)

lazy val cluster = (project in file("cluster")).Add
  aggregate(clusterShared, kernel, worker)

lazy val examples = (project in file("examples")).aggregate(
  exampleJobs, exampleProducers
)

lazy val common = (project in file("common")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.common
  )

lazy val network = (project in file("network")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.network
  ).
  dependsOn(common)

lazy val client = (project in file("client")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.client
  ).
  dependsOn(network)

lazy val clusterShared = Project("cluster-shared", file("cluster/shared")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.cluster
  ).
  dependsOn(network)

lazy val kernel = MultiNode(Project("cluster-kernel", file("cluster/kernel"))).
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.serverJvm,
    parallelExecution in Test := false
  ).
  enablePlugins(JavaServerAppPackaging).
  settings(Packaging.universalServerSettings: _*).
  enablePlugins(DockerPlugin).
  settings(Packaging.schedulerDockerSettings: _*).
  dependsOn(clusterShared).
  dependsOn(consoleJVM)

lazy val worker = Project("cluster-worker", file("cluster/worker")).
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.worker
  ).
  enablePlugins(JavaServerAppPackaging).
  enablePlugins(DockerPlugin).
  settings(Packaging.universalServerSettings: _*).
  settings(Packaging.workerDockerSettings: _*).
  dependsOn(clusterShared)

lazy val consoleRoot = (project in file("console")).
  aggregate(consoleJS, consoleJVM)

lazy val console = (crossProject in file("console")).
  settings(commonSettings: _*).settings(
  name := "console",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "scalatags" % "0.4.6",
    "com.lihaoyi" %%% "upickle" % "0.3.6",
    "com.lihaoyi" %%% "utest" % "0.3.0" % "test"
  )
).jsSettings(
  libraryDependencies ++= {
    val scalaJSReactVersion = "0.9.2"
    val scalaCssVersion = "0.3.0"

    Seq(
      "biz.enef" %%% "slogging" % "0.3",
      "com.github.japgolly.scalajs-react" %%% "core" % scalaJSReactVersion,
      "com.github.japgolly.scalajs-react" %%% "extra" % scalaJSReactVersion,
      "com.github.japgolly.scalajs-react" %%% "test" % scalaJSReactVersion % "test",
      "com.github.japgolly.scalacss" %%% "core" % scalaCssVersion,
      "com.github.japgolly.scalacss" %%% "ext-react" % scalaCssVersion
    )},
  jsDependencies ++= Seq(
    "org.webjars" % "react" % "0.12.2" / "react-with-addons.js" commonJSName "React",
    "org.webjars" % "react" % "0.12.2" % "test" / "react-with-addons.js" commonJSName "React"
  ),
  requiresDOM := true,
  scalaJSStage in Test := FastOptStage,
  jsEnv in Test := new PhantomJS2Env(scalaJSPhantomJSClassLoader.value)
).jvmSettings(
  libraryDependencies ++= {
    val akkaVersion = "2.4.0"
    val akkaHttpVersion = "1.0"

    Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-core-experimental" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-upickle" % "1.1.0",
      "com.typesafe.slick" %% "slick" % "3.1.0",
      "org.scalaz" %% "scalaz-core" % "7.1.4"
    )}
)

lazy val consoleJS = console.js
lazy val consoleJVM = console.jvm.settings(
  (resources in Compile) ++= Seq(
    (fastOptJS in (consoleJS, Compile)).value.data,
    file((fastOptJS in (consoleJS, Compile)).value.data.getAbsolutePath + ".map"),
    (packageJSDependencies in (consoleJS, Compile)).value
  )
)

lazy val exampleJobs = Project("example-jobs", file("examples/jobs")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.exampleJobs
  ).
  dependsOn(common)

lazy val exampleProducers = Project("example-producers", file("examples/producers")).
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.exampleProducers
  ).
  enablePlugins(JavaAppPackaging).
  settings(Packaging.universalSettings: _*).
  enablePlugins(DockerPlugin).
  settings(Packaging.dockerSettings: _*).
  dependsOn(common).
  dependsOn(exampleJobs).
  dependsOn(client)
