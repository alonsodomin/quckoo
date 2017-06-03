import sbt._
import sbt.Keys._

import com.typesafe.sbt.pgp.PgpKeys

import scala.xml.transform.{RewriteRule, RuleTransformer}

scalaVersion in ThisBuild := "2.12.1"

parallelExecution in ThisBuild := false

lazy val sandbox  = settingKey[String]("The name of the environment sandbox to use.")
lazy val botBuild = settingKey[Boolean]("Build by TravisCI instead of local dev environment")

lazy val commonSettings = Seq(
    licenses += ("Apache-2.0", url(
      "https://www.apache.org/licenses/LICENSE-2.0.txt")),
    organization := "io.quckoo",
    organizationName := "A. Alonso Dominguez",
    startYear := Some(2015),
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-language:postfixOps",
      "-language:higherKinds",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Xlint",
      "-Xfuture",
      "-Xfatal-warnings",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ypartial-unification"
    ),
    resolvers ++= Seq(
      Opts.resolver.mavenLocalFile,
      Resolver.bintrayRepo("krasserm", "maven"),
      Resolver.bintrayRepo("hseeberger", "maven"),
      Resolver.bintrayRepo("dnvriend", "maven"),
      Resolver.bintrayRepo("tecsisa", "maven-bintray-repo"),
      Resolver.bintrayRepo("jvican", "releases")
    ),
    botBuild := scala.sys.env.get("TRAVIS").isDefined
  )

lazy val commonJvmSettings = Seq(
  fork in Test := false
)

lazy val commonJsSettings = Seq(
  coverageEnabled := false,
  coverageExcludedFiles := ".*",
  scalaJSStage in Test := FastOptStage,
  jsEnv in Test := PhantomJSEnv().value,
  // batch mode decreases the amount of memory needed to compile scala.js code
  scalaJSOptimizerOptions := scalaJSOptimizerOptions.value.withBatchMode(botBuild.value)
)

lazy val scoverageSettings = Seq(
  coverageHighlighting := true,
  coverageExcludedPackages := "io\\.quckoo\\.console\\.html\\..*"
)

lazy val instrumentationSettings = aspectjSettings ++ Seq(
  AspectjKeys.aspectjVersion in Aspectj := "1.8.10",
  AspectjKeys.sourceLevel in Aspectj := "1.8",
  javaOptions in reStart ++= (AspectjKeys.weaverOptions in Aspectj).value
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
  .settings(commonSettings)
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
    shell,
    console,
    shared,
    master,
    worker,
    examples,
    utilJS,
    utilJVM,
    testSupportJS,
    testSupportJVM
  )

// Core ==================================================

lazy val core = (crossProject.crossType(CrossType.Pure) in file("core"))
  .enablePlugins(BuildInfoPlugin, AutomateHeaderPlugin)
  .settings(
    name := "core",
    moduleName := "quckoo-core",
    buildInfoPackage := "io.quckoo",
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion),
    buildInfoObject := "Info"
  )
  .settings(commonSettings)
  .settings(scoverageSettings)
  .settings(publishSettings)
  .settings(Dependencies.core)
  .jsSettings(commonJsSettings)
  .jsSettings(Dependencies.coreJS)
  .jvmSettings(commonJvmSettings)
  .jvmSettings(Dependencies.coreJVM)
  .dependsOn(util, testSupport % Test)

lazy val coreJS = core.js
lazy val coreJVM = core.jvm

// API ==================================================

lazy val api = (crossProject.crossType(CrossType.Pure) in file("api"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(scoverageSettings)
  .settings(publishSettings)
  .settings(Dependencies.api)
  .jsSettings(commonJsSettings)
  .jvmSettings(commonJvmSettings)
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
  .settings(commonSettings)
  .settings(scoverageSettings)
  .settings(publishSettings)
  .settings(Dependencies.client)
  .jsSettings(commonJsSettings)
  .jsSettings(Dependencies.clientJS)
  .jvmSettings(commonJvmSettings)
  .jvmSettings(Dependencies.clientJVM)
  .settings(
    name := "client",
    moduleName := "quckoo-client",
    requiresDOM := true
  )
  .dependsOn(api, testSupport % Test)

lazy val clientJS = client.js
lazy val clientJVM = client.jvm

// Shell ====================================================

lazy val shell = (project in file("shell"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(scoverageSettings)
  .settings(publishSettings)
  .settings(
    name := "shell",
    moduleName := "quckoo-shell"
  )
  .settings(Dependencies.shell)
  .dependsOn(clientJVM)

// Console ==================================================

lazy val console = (project in file("console"))
  .enablePlugins(AutomateHeaderPlugin, ScalaJSPlugin, ScalaJSWeb)
  .settings(commonSettings)
  .settings(commonJsSettings)
  .settings(publishSettings)
  .settings(Dependencies.console)
  .settings(
    name := "console",
    moduleName := "quckoo-console",
    requiresDOM := true,
    scalaJSUseMainModuleInitializer in Compile := true
  )
  .dependsOn(clientJS, testSupportJS % Test)

// Server ==================================================

lazy val shared = (project in file("shared"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(commonJvmSettings)
  .settings(scoverageSettings)
  .settings(publishSettings)
  .settings(Dependencies.clusterShared)
  .settings(
    moduleName := "quckoo-shared"
  )
  .dependsOn(apiJVM, testSupportJVM % Test)

lazy val master = (project in file("master"))
  .enablePlugins(
    AutomateHeaderPlugin,
    QuckooWebServer,
    QuckooServerPackager,
    QuckooMultiJvmTesting
  )
  .settings(commonSettings)
  .settings(commonJvmSettings)
  .settings(scoverageSettings)
  .settings(publishSettings)
  .settings(automateHeaderSettings(MultiJvm))
  .settings(Dependencies.clusterMaster)
  .settings(
    moduleName := "quckoo-master",
    scalaJSProjects := Seq(console),
    dockerExposedPorts := Seq(2551, 8095)
  )
  .dependsOn(shared % "compile->compile;test->test", testSupportJVM % Test)

lazy val worker = (project in file("worker"))
  .enablePlugins(AutomateHeaderPlugin, QuckooApp, QuckooServerPackager)
  .settings(commonSettings)
  .settings(commonJvmSettings)
  .settings(scoverageSettings)
  .settings(publishSettings)
  .settings(Dependencies.clusterWorker)
  .settings(
    moduleName := "quckoo-worker",
    dockerExposedPorts := Seq(5001, 9010)
  )
  .dependsOn(shared % "compile->compile;test->test", testSupportJVM % Test)

// Misc Utilities ===========================================

lazy val util = (crossProject in file("util"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .jsSettings(commonJsSettings)
  .jsSettings(Dependencies.utilJS)
  .jvmSettings(commonJvmSettings)
  .settings(moduleName := "quckoo-util")
  .dependsOn(testSupport % Test)

lazy val utilJS = util.js
lazy val utilJVM = util.jvm

// Test Support Utilities ===================================

lazy val testSupport = (crossProject in file("test-support"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(Dependencies.testSupport)
  .jsSettings(commonJsSettings)
  .jvmSettings(commonJvmSettings)
  .jvmSettings(Dependencies.testSupportJVM)
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
  .settings(commonSettings)
  .settings(commonJvmSettings)
  .settings(publishSettings)
  .settings(Dependencies.exampleJobs)
  .settings(
    name := "example-jobs",
    moduleName := "quckoo-example-jobs"
  )
  .dependsOn(coreJVM)

lazy val exampleProducers = (project in file("examples/producers"))
  .enablePlugins(AutomateHeaderPlugin, QuckooAppPackager)
  .settings(commonSettings)
  .settings(commonJvmSettings)
  .settings(publishSettings)
  .settings(Revolver.settings)
  .settings(Dependencies.exampleProducers)
  .settings(
    name := "example-producers",
    moduleName := "quckoo-example-producers"
  )
  .dependsOn(clientJVM, exampleJobs)

// Command aliases ==================================================

addCommandAlias("testJS",
                ";coreJS/test;apiJS/test;clientJS/test;console/test")
addCommandAlias("validate", ";test;master/multi-jvm:test")
