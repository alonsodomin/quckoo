import sbt.Keys._
import sbt._

lazy val commonSettings = Seq(
  organization := "io.kairos",
  version := "0.1.0-SNAPSHOT",
  licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")),
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-Xexperimental",
    "-language:postfixOps",
    "-language:higherKinds",
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
  coverageHighlighting := true,
  coverageExcludedPackages := "io\\.kairos\\.console\\.client\\..*"
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
  settings(scoverageSettings: _*).
  settings(Dependencies.common: _*).
  jsSettings(Dependencies.commonJS: _*).
  jvmSettings(Dependencies.commonJVM: _*)

lazy val commonJS = common.js
lazy val commonJVM = common.jvm

lazy val client = (project in file("client")).
  settings(commonSettings: _*).
  settings(Dependencies.client: _*).
  dependsOn(commonJVM)

// Console ==================================================

lazy val consoleRoot = (project in file("console")).
  settings(moduleName := "console").
  settings(noPublishSettings).
  aggregate(consoleJS, consoleJVM, consoleResources)

lazy val console = (crossProject in file("console")).
  settings(commonSettings: _*).
  settings(name := "console").
  settings(Dependencies.console: _*).
  jsSettings(Dependencies.consoleJS: _*).
  jsSettings(
    requiresDOM := true,
    coverageEnabled := false,
    persistLauncher in Compile := true,
    persistLauncher in Test := false,
    scalaJSStage in Test := FastOptStage,
    jsEnv in Test := new PhantomJS2Env(scalaJSPhantomJSClassLoader.value)
  )
  .jvmSettings(Dependencies.consoleJVM: _*).
  dependsOn(common)

lazy val consoleJS = console.js
lazy val consoleJVM = console.jvm

lazy val consoleResources = (project in file("console/resources")).
  aggregate(consoleJS).
  enablePlugins(SbtSass).
  settings(commonSettings: _*).
  settings(Dependencies.consoleResources).
  settings(
    name := "console-resources",
    exportJars := true,
    unmanagedResourceDirectories in Compile += (crossTarget in consoleJS).value,
    includeFilter in (Compile, unmanagedResources) := ("*.js" || "*.css" || "*.js.map"),
    mappings in (Compile, packageBin) ~= { (ms: Seq[(File, String)]) =>
      ms.filter(!_._1.getName.endsWith("scss")).map { case (file, path) =>
        val prefix = {
          if (file.getName.indexOf(".css") >= 0) "css/"
          else if (file.getName.indexOf(".js") >= 0) "js/"
          else ""
        }
        (file, s"kairos/$prefix${file.getName}")
      }
    },
    mappings in (Compile, packageBin) <++= (WebKeys.webJarsDirectory in Assets).map { path =>
      val fontPaths = Seq(
        path / "lib" / "font-awesome" / "fonts",
        path / "lib" / "bootstrap-sass" / "fonts" / "bootstrap"
      )

      fontPaths.flatMap { p =>
        p.listFiles().map { src => (src, "kairos/fonts/" + src.getName) }
      }
    },
    packageBin in Compile <<= (packageBin in Compile) dependsOn ((fastOptJS in Compile) in consoleJS)
  )

// Cluster ==================================================

lazy val cluster = (project in file("cluster")).
  settings(noPublishSettings).
  settings(scoverageSettings).
  aggregate(clusterShared, master, worker)

lazy val clusterShared = Project("cluster-shared", file("cluster/shared")).
  settings(commonSettings).
  settings(Dependencies.clusterShared).
  dependsOn(commonJVM)

lazy val master = MultiNode(Project("cluster-master", file("cluster/master"))).
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(Dependencies.clusterMaster).
  settings(
    reStart <<= reStart dependsOn ((packageBin in Compile) in consoleResources)
  ).
  enablePlugins(JavaServerAppPackaging, DockerPlugin).
  settings(Packaging.universalServerSettings: _*).
  settings(Packaging.masterDockerSettings: _*).
  dependsOn(clusterShared, consoleResources, consoleJVM)

lazy val worker = Project("cluster-worker", file("cluster/worker")).
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(Dependencies.clusterWorker).
  enablePlugins(JavaServerAppPackaging, DockerPlugin).
  settings(Packaging.universalServerSettings: _*).
  settings(Packaging.workerDockerSettings: _*).
  dependsOn(clusterShared)

// Examples ==================================================

lazy val examples = (project in file("examples")).
  aggregate(exampleJobs, exampleProducers).
  settings(noPublishSettings)

lazy val exampleJobs = Project("example-jobs", file("examples/jobs")).
  settings(commonSettings: _*).
  settings(Dependencies.exampleJobs).
  dependsOn(commonJVM)

lazy val exampleProducers = Project("example-producers", file("examples/producers")).
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(Dependencies.exampleProducers).
  enablePlugins(JavaAppPackaging, DockerPlugin).
  settings(Packaging.universalSettings: _*).
  settings(Packaging.dockerSettings: _*).
  dependsOn(commonJVM, exampleJobs, client)

// Command aliases ==================================================

addCommandAlias("testJS", ";commonJS/test;consoleJS/test")
addCommandAlias("testJVM", ";commonJVM/test;client/test;consoleJVM/test;cluster/test;examples/test")