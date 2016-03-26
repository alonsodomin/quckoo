import sbt.Keys._
import sbt._

lazy val commonSettings = Seq(
  organization := "io.quckoo",
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

lazy val commonJsSettings = Seq(
  coverageEnabled := false,
  persistLauncher in Compile := true,
  persistLauncher in Test := false,
  scalaJSStage in Test := FastOptStage,
  jsEnv in Test := new PhantomJS2Env(scalaJSPhantomJSClassLoader.value)
)

lazy val scoverageSettings = Seq(
  coverageHighlighting := true,
  coverageExcludedPackages := "io\\.quckoo\\.console\\.client\\..*"
)

lazy val quckoo = (project in file(".")).
  settings(moduleName := "quckoo-root").
  settings(noPublishSettings).
  aggregate(commonRoot, apiJS, apiJVM, client, cluster, consoleRoot, examples)

// Common ==================================================

lazy val commonRoot = (project in file("common")).
  settings(moduleName := "quckoo-common-root").
  settings(noPublishSettings).
  aggregate(commonJS, commonJVM)

lazy val common = (crossProject in file("common")).
  settings(
    name := "common",
    moduleName := "quckoo-common"
  ).
  settings(commonSettings: _*).
  settings(scoverageSettings: _*).
  settings(Dependencies.common: _*).
  jsSettings(commonJsSettings: _*).
  jsSettings(Dependencies.commonJS: _*).
  jvmSettings(Dependencies.commonJVM: _*)

lazy val commonJS = common.js
lazy val commonJVM = common.jvm

// API ==================================================

lazy val api = (crossProject.crossType(CrossType.Pure) in file("api")).
  settings(
    name := "api",
    moduleName := "quckoo-api"
  ).
  settings(commonSettings: _*).
  settings(Dependencies.api: _*).
  jsSettings(commonJsSettings: _*).
  dependsOn(common)

lazy val apiJS = api.js
lazy val apiJVM = api.jvm

// Client ==================================================

lazy val client = (project in file("client")).
  settings(moduleName := "quckoo-client").
  settings(commonSettings: _*).
  settings(Dependencies.client: _*).
  dependsOn(apiJVM)

// Console ==================================================

lazy val consoleRoot = (project in file("console")).
  settings(moduleName := "quckoo-console-root").
  settings(noPublishSettings).
  aggregate(consoleApp)

lazy val consoleApp = (project in file("console/app")).
  enablePlugins(ScalaJSPlugin).
  settings(
    name := "console",
    moduleName := "quckoo-console-app",
    requiresDOM := true
  ).
  settings(commonSettings: _*).
  settings(commonJsSettings: _*).
  settings(Dependencies.consoleApp: _*).
  dependsOn(commonJS, apiJS)

/*
lazy val consoleResources = (project in file("console/resources")).
  aggregate(consoleApp).
  enablePlugins(SbtSass).
  settings(commonSettings: _*).
  settings(Dependencies.consoleResources).
  settings(
    name := "console-resources",
    moduleName := "quckoo-console-resources",
    exportJars := true,
    unmanagedResourceDirectories in Compile += (crossTarget in consoleApp).value,
    includeFilter in (Compile, unmanagedResources) := ("*.js" || "*.css" || "*.js.map"),
    mappings in (Compile, packageBin) ~= { (ms: Seq[(File, String)]) =>
      ms.filter(!_._1.getName.endsWith("scss")).map { case (file, path) =>
        val prefix = {
          if (file.getName.indexOf(".css") >= 0) "css/"
          else if (file.getName.indexOf(".js") >= 0) "js/"
          else ""
        }
        (file, s"quckoo/$prefix${file.getName}")
      }
    },
    mappings in (Compile, packageBin) <++= (WebKeys.webJarsDirectory in Assets).map { path =>
      val fontPaths = Seq(
        path / "lib" / "font-awesome" / "fonts",
        path / "lib" / "bootstrap-sass" / "fonts" / "bootstrap"
      )

      fontPaths.flatMap { p =>
        p.listFiles().map { src => (src, "quckoo/fonts/" + src.getName) }
      }
    },
    packageBin in Compile <<= (packageBin in Compile) dependsOn ((fastOptJS in Compile) in consoleApp)
  )
*/

// Cluster ==================================================

lazy val cluster = (project in file("cluster")).
  settings(name := "quckoo-cluster").
  settings(noPublishSettings).
  settings(scoverageSettings).
  aggregate(clusterShared, clusterMaster, clusterWorker)

lazy val clusterShared = (project in file("cluster/shared")).
  settings(moduleName := "quckoo-cluster-shared").
  settings(commonSettings).
  settings(Dependencies.clusterShared).
  dependsOn(apiJVM)

lazy val clusterMaster = MultiNode(project in file("cluster/master")).
  settings(moduleName := "quckoo-cluster-master").
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(Dependencies.clusterMaster).
  settings(
    //reStart <<= reStart dependsOn ((packageBin in Compile) in consoleResources)
  ).
  enablePlugins(JavaServerAppPackaging, DockerPlugin).
  settings(Packaging.universalServerSettings: _*).
  settings(Packaging.masterDockerSettings: _*).
  dependsOn(clusterShared)

lazy val clusterWorker = (project in file("cluster/worker")).
  settings(moduleName := "quckoo-cluster-worker").
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

lazy val exampleJobs = (project in file("examples/jobs")).
  settings(moduleName := "quckoo-example-jobs").
  settings(commonSettings: _*).
  settings(Dependencies.exampleJobs).
  dependsOn(commonJVM)

lazy val exampleProducers = (project in file("examples/producers")).
  settings(moduleName := "quckoo-example-producers").
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(Dependencies.exampleProducers).
  enablePlugins(JavaAppPackaging, DockerPlugin).
  settings(Packaging.universalSettings: _*).
  settings(Packaging.dockerSettings: _*).
  dependsOn(client, exampleJobs)

// Command aliases ==================================================

addCommandAlias("testJS", ";commonJS/test;consoleJS/test")
addCommandAlias("testJVM", ";commonJVM/test;client/test;consoleJVM/test;cluster/test;examples/test")