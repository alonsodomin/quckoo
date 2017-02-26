import com.typesafe.sbt.pgp.PgpKeys
import sbt.Keys._
import sbt._

import scala.xml.transform.{RewriteRule, RuleTransformer}

organization in ThisBuild := "io.quckoo"

scalaVersion in ThisBuild := "2.12.1"

val sandbox = settingKey[String]("The name of the environment sandbox to use.")

lazy val commonSettings = Seq(
    licenses += ("Apache-2.0", url(
      "http://opensource.org/licenses/Apache-2.0")),
    startYear := Some(2015),
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-Xexperimental",
      "-language:postfixOps",
      "-language:higherKinds",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Xlint",
      "-Xfuture",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Xfatal-warnings"
    ),
    resolvers ++= Seq(
      Opts.resolver.mavenLocalFile,
      Resolver.bintrayRepo("krasserm", "maven"),
      Resolver.bintrayRepo("hseeberger", "maven"),
      Resolver.bintrayRepo("dnvriend", "maven"),
      Resolver.bintrayRepo("tecsisa", "maven-bintray-repo")
    ),
    parallelExecution in Test := false
  ) ++ Licensing.settings

lazy val commonJsSettings = Seq(
  coverageEnabled := false,
  coverageExcludedFiles := ".*",
  persistLauncher in Test := false,
  scalaJSStage in Test := FastOptStage,
  jsEnv in Test := PhantomJSEnv().value
)

lazy val scoverageSettings = Seq(
  coverageHighlighting := true,
  coverageExcludedPackages := "io\\.quckoo\\.console\\.html\\..*"
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val publishSettings = Seq(
  homepage := Some(url("https://www.quckoo.io")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishTo := Some(
    if (isSnapshot.value) Opts.resolver.sonatypeSnapshots
    else Opts.resolver.sonatypeStaging
  ),
  // don't include scoverage as a dependency in the pom
  // see issue #980
  // this code was copied from https://github.com/mongodb/mongo-spark
  pomPostProcess := { (node: xml.Node) =>
    new RuleTransformer(new RewriteRule {
      override def transform(node: xml.Node): Seq[xml.Node] = node match {
        case e: xml.Elem
            if e.label == "dependency" && e.child.exists(child =>
              child.label == "groupId" && child.text == "org.scoverage") =>
          Nil
        case _ => Seq(node)
      }
    }).transform(node).head
  },
  pomExtra :=
    <scm>
      <url>git@github.com:alonsodomin/quckoo.git</url>
      <connection>scm:git:git@github.com:alonsodomin/quckoo.git</connection>
    </scm>
    <developers>
      <developer>
        <id>alonsodomin</id>
        <name>Antonio Alonso Dominguez</name>
        <url>https://github.com/alonsodomin</url>
      </developer>
    </developers>
)

lazy val releaseSettings = {
  import ReleaseTransformations._

  val sonatypeReleaseAll = ReleaseStep(
    action = Command.process("sonatypeReleaseAll", _))
  val dockerRelease = ReleaseStep(action = st => {
    val extracted = Project.extract(st)
    val projectRef: ProjectRef = extracted.get(thisProjectRef)
    extracted.runAggregated(publish in Docker in projectRef, st)
    st
  })

  Seq(
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      sonatypeReleaseAll,
      dockerRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )
}

lazy val quckoo = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin, DockerComposePlugin)
  .settings(
    name := "quckoo",
    moduleName := "quckoo-root",
    sandbox := "standalone",
    dockerImageCreationTask := (publishLocal in Docker).value,
    composeFile := s"./sandbox/${sandbox.value}/docker-compose.yml"
  )
  .settings(noPublishSettings)
  .settings(releaseSettings)
  .aggregate(
    coreJS,
    coreJVM,
    apiJS,
    apiJVM,
    clientJS,
    clientJVM,
    cluster,
    console,
    examples,
    utilJS,
    utilJVM,
    testSupportJS,
    testSupportJVM
  )

// Core ==================================================

lazy val core = (crossProject.crossType(CrossType.Pure) in file("core"))
  .enablePlugins(BuildInfoPlugin, AutomateHeaderPlugin)
  .settings(commonSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(publishSettings: _*)
  .settings(Dependencies.core: _*)
  .jsSettings(commonJsSettings: _*)
  .settings(
    name := "core",
    moduleName := "quckoo-core",
    buildInfoPackage := "io.quckoo",
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion),
    buildInfoObject := "Info"
  )
  .dependsOn(util, testSupport % Test)

lazy val coreJS = core.js
lazy val coreJVM = core.jvm

// API ==================================================

lazy val api = (crossProject.crossType(CrossType.Pure) in file("api"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(publishSettings: _*)
  .settings(Dependencies.api: _*)
  .jsSettings(commonJsSettings: _*)
  .settings(
    name := "api",
    moduleName := "quckoo-api"
  )
  .dependsOn(core, testSupport % Test)

lazy val apiJS = api.js
lazy val apiJVM = api.jvm

// Client ==================================================

lazy val client = (crossProject in file("client"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(publishSettings: _*)
  .settings(Dependencies.client: _*)
  .jsSettings(commonJsSettings: _*)
  .jsSettings(Dependencies.clientJS: _*)
  .jvmSettings(Dependencies.clientJVM: _*)
  .settings(
    name := "client",
    moduleName := "quckoo-client",
    requiresDOM := true
  )
  .dependsOn(api, testSupport % Test)

lazy val clientJS = client.js
lazy val clientJVM = client.jvm

// Console ==================================================

lazy val console = (project in file("console"))
  .enablePlugins(AutomateHeaderPlugin, ScalaJSPlugin, ScalaJSWeb)
  .settings(commonSettings: _*)
  .settings(commonJsSettings: _*)
  .settings(publishSettings: _*)
  .settings(Dependencies.console: _*)
  .settings(
    name := "console",
    moduleName := "quckoo-console",
    requiresDOM := true,
    persistLauncher in Compile := true
  )
  .dependsOn(clientJS, testSupportJS % Test)

// Cluster ==================================================

lazy val cluster = (project in file("cluster"))
  .settings(name := "quckoo-cluster")
  .settings(noPublishSettings)
  .settings(scoverageSettings)
  .aggregate(clusterShared, clusterMaster, clusterWorker)

lazy val clusterShared = (project in file("cluster/shared"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(publishSettings: _*)
  .settings(Dependencies.clusterShared)
  .settings(
    name := "cluster-shared",
    moduleName := "quckoo-cluster-shared"
  )
  .dependsOn(apiJVM, testSupportJVM % Test)

lazy val clusterMaster = (project in file("cluster/master"))
  .enablePlugins(
    AutomateHeaderPlugin,
    SbtSass,
    SbtTwirl,
    JavaServerAppPackaging,
    DockerPlugin
  )
  .configs(MultiJvm)
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(Revolver.settings: _*)
  .settings(Dependencies.clusterMaster)
  .settings(MultiNode.settings)
  .settings(Packaging.masterSettings: _*)
  .settings(
    name := "cluster-master",
    moduleName := "quckoo-cluster-master",
    scalaJSProjects := Seq(console),
    baseDirectory in reStart := file("cluster/master/target"),
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value,
    pipelineStages in Assets := Seq(scalaJSPipeline),
    devCommands in scalaJSPipeline ++= Seq("test", "testQuick", "testOnly", "docker:publishLocal")
  )
  .dependsOn(clusterShared, testSupportJVM % Test)

lazy val clusterWorker = (project in file("cluster/worker"))
  .enablePlugins(AutomateHeaderPlugin, JavaServerAppPackaging, DockerPlugin)
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(Revolver.settings: _*)
  .settings(Dependencies.clusterWorker)
  .settings(Packaging.workerSettings: _*)
  .settings(
    name := "cluster-worker",
    moduleName := "quckoo-cluster-worker",
    baseDirectory in reStart := file("cluster/worker/target")
  )
  .dependsOn(clusterShared, testSupportJVM % Test)

// Misc Utilities ===========================================

lazy val util = (crossProject in file("util"))
  .settings(commonSettings)
  .jsSettings(Dependencies.utilJS)
  .settings(moduleName := "quckoo-util")
  .dependsOn(testSupport % Test)

lazy val utilJS = util.js
lazy val utilJVM = util.jvm

// Test Support Utilities ===================================

lazy val testSupport = (crossProject in file("test-support"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(Dependencies.testSupport: _*)
  .jsSettings(commonJsSettings: _*)
  .jvmSettings(Dependencies.testSupportJVM: _*)
  .settings(
    name := "test-support",
    moduleName := "quckoo-test-support"
  )

lazy val testSupportJS = testSupport.js
lazy val testSupportJVM = testSupport.jvm

// Examples ==================================================

lazy val examples = (project in file("examples"))
  .settings(moduleName := "quckoo-examples")
  .aggregate(exampleJobs, exampleProducers)
  .settings(noPublishSettings)

lazy val exampleJobs = (project in file("examples/jobs"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(Dependencies.exampleJobs)
  .settings(
    name := "example-jobs",
    moduleName := "quckoo-example-jobs"
  )
  .dependsOn(coreJVM)

lazy val exampleProducers = (project in file("examples/producers"))
  .enablePlugins(AutomateHeaderPlugin, JavaAppPackaging, DockerPlugin)
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(Revolver.settings: _*)
  .settings(Dependencies.exampleProducers)
  .settings(Packaging.exampleProducersSettings: _*)
  .settings(
    name := "example-producers",
    moduleName := "quckoo-example-producers"
  )
  .dependsOn(clientJVM, exampleJobs)

// Command aliases ==================================================

addCommandAlias("testJS",
                ";coreJS/test;apiJS/test;clientJS/test;consoleApp/test")
addCommandAlias("validate", ";test;multi-jvm:test")
