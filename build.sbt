import sbt.Keys._
import sbt._

organization in ThisBuild := "io.quckoo"

scalaVersion in ThisBuild := "2.11.8"

lazy val commonSettings = Seq(
  licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")),
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-Xexperimental",
    "-language:postfixOps",
    "-language:higherKinds",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-Ywarn-dead-code",
    "-Xfatal-warnings"
  ),
  resolvers ++= Seq(
    Opts.resolver.mavenLocalFile,
    Resolver.bintrayRepo("krasserm", "maven"),
    Resolver.bintrayRepo("hseeberger", "maven"),
    Resolver.bintrayRepo("dnvriend", "maven")
  ),
  parallelExecution in Test := false
) ++ Licensing.settings

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val commonJsSettings = Seq(
  coverageEnabled := false,
  coverageExcludedFiles := ".*",
  persistLauncher in Compile := true,
  persistLauncher in Test := false,
  scalaJSStage in Test := FastOptStage,
  scalaJSUseRhino in Global := false,
  jsEnv in Test := new PhantomJS2Env(scalaJSPhantomJSClassLoader.value)
)

lazy val scoverageSettings = Seq(
  coverageHighlighting := true,
  coverageExcludedPackages := "io\\.quckoo\\.console\\.client\\..*"
)

lazy val quckoo = (project in file(".")).
  settings(moduleName := "quckoo-root").
  settings(noPublishSettings).
  enablePlugins(AutomateHeaderPlugin).
  aggregate(coreJS, coreJVM, apiJS, apiJVM, clientJS, clientJVM, cluster, console, examples)

// Core ==================================================

lazy val core = (crossProject.crossType(CrossType.Pure) in file("core")).
  settings(
    name := "core",
    moduleName := "quckoo-core"
  ).
  settings(commonSettings: _*).
  settings(scoverageSettings: _*).
  settings(Dependencies.core: _*).
  jsSettings(commonJsSettings: _*).
  jsSettings(Dependencies.coreJS: _*)

lazy val coreJS = core.js
lazy val coreJVM = core.jvm

// API ==================================================

lazy val api = (crossProject.crossType(CrossType.Pure) in file("api")).
  settings(
    name := "api",
    moduleName := "quckoo-api"
  ).
  settings(commonSettings: _*).
  settings(Dependencies.api: _*).
  jsSettings(commonJsSettings: _*).
  dependsOn(core)

lazy val apiJS = api.js
lazy val apiJVM = api.jvm

// Client ==================================================

lazy val client = (crossProject in file("client")).
  settings(
    name := "client",
    moduleName := "quckoo-client",
    requiresDOM := true
  ).
  settings(commonSettings: _*).
  settings(scoverageSettings: _*).
  settings(Dependencies.client: _*).
  jsSettings(commonJsSettings: _*).
  jsSettings(Dependencies.clientJS: _*).
  jvmSettings(Dependencies.clientJVM: _*).
  dependsOn(api)

lazy val clientJS = client.js
lazy val clientJVM = client.jvm

// Console ==================================================

lazy val console = (project in file("console")).
  settings(moduleName := "quckoo-console").
  settings(noPublishSettings).
  aggregate(consoleApp, consoleResources)

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
  dependsOn(coreJS, apiJS, clientJS)

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
    excludeFilter in (Compile, unmanagedResources) := "index.js",
    mappings in (Compile, packageBin) ~= { (ms: Seq[(File, String)]) =>
      ms.filter { case (file, _) => !file.getName.endsWith("scss") }.map { case (file, path) =>
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
        path / "lib" / "font-awesome"   / "fonts",
        path / "lib" / "bootstrap-sass" / "fonts" / "bootstrap"
      )

      fontPaths.flatMap { p =>
        p.listFiles().map { src => (src, "quckoo/fonts/" + src.getName) }
      }
    },
    packageBin in Compile <<= (packageBin in Compile) dependsOn ((fastOptJS in Compile) in consoleApp)
  )

// Cluster ==================================================

lazy val cluster = (project in file("cluster")).
  settings(name := "quckoo-cluster").
  settings(noPublishSettings).
  settings(scoverageSettings).
  aggregate(clusterShared, clusterMaster, clusterWorker)

lazy val clusterShared = (project in file("cluster/shared")).
  settings(
    name := "cluster-shared",
    moduleName := "quckoo-cluster-shared"
  ).
  settings(commonSettings).
  settings(Dependencies.clusterShared).
  dependsOn(apiJVM)

lazy val clusterMaster = (project in file("cluster/master")).
  configs(MultiJvm).
  settings(
    name := "cluster-master",
    moduleName := "quckoo-cluster-master"
  ).
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(Dependencies.clusterMaster).
  settings(MultiNode.settings).
  settings(
    //reStart <<= reStart dependsOn ((packageBin in Compile) in consoleResources)
  ).
  enablePlugins(JavaServerAppPackaging, DockerPlugin).
  settings(Packaging.universalServerSettings: _*).
  settings(Packaging.masterDockerSettings: _*).
  dependsOn(clusterShared, consoleResources)

lazy val clusterWorker = (project in file("cluster/worker")).
  settings(
    name := "cluster-worker",
    moduleName := "quckoo-cluster-worker"
  ).
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(Dependencies.clusterWorker).
  enablePlugins(JavaServerAppPackaging, DockerPlugin).
  settings(Packaging.universalServerSettings: _*).
  settings(Packaging.workerDockerSettings: _*).
  dependsOn(clusterShared)

// Examples ==================================================

lazy val examples = (project in file("examples")).
  settings(moduleName := "quckoo-examples").
  aggregate(exampleJobs, exampleProducers).
  settings(noPublishSettings)

lazy val exampleJobs = (project in file("examples/jobs")).
  settings(
    name := "example-jobs",
    moduleName := "quckoo-example-jobs"
  ).
  settings(commonSettings: _*).
  settings(Dependencies.exampleJobs).
  dependsOn(coreJVM)

lazy val exampleProducers = (project in file("examples/producers")).
  settings(
    name := "example-producers",
    moduleName := "quckoo-example-producers"
  ).
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(Dependencies.exampleProducers).
  enablePlugins(JavaAppPackaging, DockerPlugin).
  settings(Packaging.universalSettings: _*).
  settings(Packaging.baseDockerSettings: _*).
  dependsOn(clientJVM, exampleJobs)

// Command aliases ==================================================

//addCommandAlias("testJS", ";commonJS/test;consoleJS/test")
//addCommandAlias("testJVM", ";commonJVM/test;client/test;consoleJVM/test;cluster/test;examples/test")
