
organization in ThisBuild := "io.kairos"

version in ThisBuild := Commons.kairosVersion

scalaVersion in ThisBuild := "2.11.7"

scalacOptions in ThisBuild ++= Seq(
  "-Xexperimental",
  "-language:postfixOps",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Ywarn-dead-code"
)

resolvers in ThisBuild ++= Seq(
  Opts.resolver.mavenLocalFile,
  "hseeberger at bintray" at "http://dl.bintray.com/hseeberger/maven",
  "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/",
  "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven",
  "OJO Snapshots" at "https://oss.jfrog.org/oss-snapshot-local"
)

coverageHighlighting in ThisBuild := true

lazy val kairos = (project in file(".")).aggregate(
  common, network, resolver, client, cluster, scheduler, consoleRoot, examples, worker
)

lazy val examples = (project in file("examples")).aggregate(
  exampleJobs, exampleProducers
)

lazy val common = (project in file("common")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.commonLibs
  )

lazy val network = (project in file("network")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.networkLibs
  ).
  dependsOn(common)

lazy val resolver = (project in file("resolver")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.resolverLibs
  ).
  dependsOn(common).
  dependsOn(network)

lazy val client = (project in file("client")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.clientLibs
  ).
  dependsOn(common).
  dependsOn(network)

lazy val cluster = (project in file("cluster")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.clusterLibs
  ).
  dependsOn(common).
  dependsOn(network)

lazy val scheduler = MultiNode(project in file("scheduler")).
  settings(Commons.settings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.schedulerLibs,
    parallelExecution in Test := false
  ).
  enablePlugins(JavaServerAppPackaging).
  settings(Packaging.universalServerSettings: _*).
  enablePlugins(DockerPlugin).
  settings(Packaging.schedulerDockerSettings: _*).
  dependsOn(common).
  dependsOn(network).
  dependsOn(resolver).
  dependsOn(cluster)

lazy val worker = (project in file("worker")).
  settings(Commons.settings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.workerLibs
  ).
  enablePlugins(JavaServerAppPackaging).
  settings(Packaging.universalServerSettings: _*).
  enablePlugins(DockerPlugin).
  settings(Packaging.workerDockerSettings: _*).
  dependsOn(common).
  dependsOn(network).
  dependsOn(resolver).
  dependsOn(cluster)

lazy val consoleRoot = (project in file("console")).
  aggregate(consoleJS, consoleJVM)

lazy val console = (crossProject in file("console")).
  settings(
  name := "kairos-ui",
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
      "com.github.japgolly.scalacss" %%% "core" % scalaCssVersion,
      "com.github.japgolly.scalacss" %%% "ext-react" % scalaCssVersion
    )},
  jsDependencies ++= Seq(
    "org.webjars" % "react" % "0.12.2" / "react-with-addons.js" commonJSName "React"
  )
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
).settings(Revolver.settings: _*)

lazy val exampleJobs = Project("example-jobs", file("examples/jobs")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.exampleJobsLibs
  ).
  dependsOn(common)

lazy val exampleProducers = Project("example-producers", file("examples/producers")).
  settings(Commons.settings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.exampleProducersLibs
  ).
  enablePlugins(JavaAppPackaging).
  settings(Packaging.universalSettings: _*).
  enablePlugins(DockerPlugin).
  settings(Packaging.dockerSettings: _*).
  dependsOn(common).
  dependsOn(exampleJobs).
  dependsOn(client)
