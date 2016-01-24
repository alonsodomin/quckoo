import sbt.Keys._
import sbt._

lazy val commonSettings = Seq(
  organization := "io.kairos",
  version := "0.1.0-SNAPSHOT",
  licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")),
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-Xexperimental",
    "-language:postfixOps",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-Ywarn-dead-code"
  ),
  resolvers ++= Seq(
    Opts.resolver.mavenLocalFile,
    Resolver.bintrayRepo("krasserm", "maven"),
    Resolver.bintrayRepo("hseeberger", "maven"),
    Resolver.bintrayRepo("dnvriend", "maven")
  ),
  parallelExecution in Test := false
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val commonJsSettins = Seq(
  scalaJSStage in Test := FastOptStage
)

lazy val scoverageSettings = Seq(
  coverageHighlighting := true
)

lazy val kairos = (project in file(".")).
  settings(moduleName := "root").
  settings(noPublishSettings).
  aggregate(commonRoot, client, cluster, consoleRoot, examples, worker)

// Common ==================================================

lazy val commonRoot = (project in file("common")).
  settings(moduleName := "common").
  settings(noPublishSettings).
  aggregate(commonJS, commonJVM)

lazy val common = (crossProject in file("common")).
  settings(
    name := "common"
  ).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle" % "0.3.6",
      "org.scalatest" %%% "scalatest" % Dependencies.version.scalaTest % Test
    )
  ).
  jsSettings(
    libraryDependencies ++= {
      import Dependencies.version._

      Seq(
        "io.github.widok" %%% "scala-js-momentjs" % "0.1.4",
        "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % scalaz
      )
    }
  ).jvmSettings(
    libraryDependencies += Dependencies.libs.scalaz
  )

lazy val commonJS = common.js
lazy val commonJVM = common.jvm

lazy val client = (project in file("client")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.client
  ).
  dependsOn(commonJVM)

// Cluster ==================================================

lazy val cluster = (project in file("cluster")).
  aggregate(clusterShared, kernel, worker).
  settings(noPublishSettings)

lazy val clusterShared = Project("cluster-shared", file("cluster/shared")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.cluster
  ).
  dependsOn(commonJVM)

lazy val kernel = MultiNode(Project("cluster-kernel", file("cluster/kernel"))).
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.kernel
  ).
  enablePlugins(JavaServerAppPackaging).
  settings(Packaging.universalServerSettings: _*).
  enablePlugins(DockerPlugin).
  settings(Packaging.kernelDockerSettings: _*).
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

// Console ==================================================

lazy val consoleRoot = (project in file("console")).
  settings(moduleName := "console").
  settings(noPublishSettings).
  aggregate(consoleJS, consoleJVM)

lazy val console = (crossProject in file("console")).
  settings(commonSettings: _*).
  settings(
    name := "console",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.4.6",
      "com.lihaoyi" %%% "upickle" % "0.3.6",
      "com.lihaoyi" %%% "utest" % "0.3.0" % "test"
    )
  ).
  jsSettings(
    libraryDependencies ++= {
      import Dependencies.version._

      Seq(
        "biz.enef" %%% "slogging" % "0.3",
        "com.github.japgolly.scalajs-react" %%% "core" % scalaJsReact,
        "com.github.japgolly.scalajs-react" %%% "extra" % scalaJsReact,
        "com.github.japgolly.scalajs-react" %%% "ext-scalaz71" % scalaJsReact,
        "com.github.japgolly.scalajs-react" %%% "ext-monocle" % scalaJsReact,
        "com.github.japgolly.scalajs-react" %%% "test" % scalaJsReact % "test",
        "com.github.japgolly.scalacss" %%% "core" % scalaCss,
        "com.github.japgolly.scalacss" %%% "ext-react" % scalaCss,

        "com.github.japgolly.fork.monocle" %%% "monocle-macro" % monocle
      )
    },
    jsDependencies ++= {
      import Dependencies.version._

      Seq(
        "org.webjars.bower" % "react" % reactJs / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
        "org.webjars.bower" % "react" % reactJs / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM",
        "org.webjars.bower" % "react" % reactJs % "test" / "react-with-addons.js" commonJSName "React"
      )
    },
    requiresDOM := true,
    coverageEnabled := false,
    scalaJSStage in Test := FastOptStage,
    jsEnv in Test := new PhantomJS2Env(scalaJSPhantomJSClassLoader.value)
  )
  .jvmSettings(
    libraryDependencies ++= {
      import Dependencies.libs._

      Seq(Akka.http, Akka.httpUpickle, Akka.sse)
    }
  ).
  dependsOn(common)

lazy val consoleJS = console.js
lazy val consoleJVM = console.jvm.settings(
  (resources in Compile) ++= Seq(
    (fastOptJS in (consoleJS, Compile)).value.data,
    file((fastOptJS in (consoleJS, Compile)).value.data.getAbsolutePath + ".map"),
    (packageJSDependencies in (consoleJS, Compile)).value
  )
)

// Examples ==================================================

lazy val examples = (project in file("examples")).
  aggregate(exampleJobs, exampleProducers).
  settings(noPublishSettings)

lazy val exampleJobs = Project("example-jobs", file("examples/jobs")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.exampleJobs
  ).
  dependsOn(commonJVM)

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
  dependsOn(commonJVM).
  dependsOn(exampleJobs).
  dependsOn(client)
