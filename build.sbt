import com.typesafe.sbt.pgp.PgpKeys
import sbt.Keys._
import sbt._

import scala.xml.transform.{RewriteRule, RuleTransformer}

organization in ThisBuild := "io.quckoo"

scalaVersion in ThisBuild := "2.11.8"

lazy val commonSettings = Seq(
    licenses += ("Apache-2.0", url(
      "http://opensource.org/licenses/Apache-2.0")),
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
      "-Ywarn-dead-code",
      "-Xfatal-warnings"
    ),
    resolvers ++= Seq(
      Opts.resolver.mavenLocalFile,
      Resolver.bintrayRepo("krasserm", "maven"),
      Resolver.bintrayRepo("hseeberger", "maven"),
      Resolver.bintrayRepo("dnvriend", "maven")
    ),
    scalafmtConfig := Some(file(".scalafmt.conf")),
    parallelExecution in Test := false
  ) ++ Licensing.settings

lazy val commonJsSettings = Seq(
  coverageEnabled := false,
  coverageExcludedFiles := ".*",
  persistLauncher in Test := false,
  scalaJSStage in Test := FastOptStage,
  scalaJSUseRhino in Global := false,
  jsEnv in Test := new PhantomJS2Env(scalaJSPhantomJSClassLoader.value)
)

lazy val scoverageSettings = Seq(
  coverageHighlighting := true,
  coverageExcludedPackages := "io\\.quckoo\\.console\\.client\\..*"
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
  .settings(
    name := "quckoo",
    moduleName := "quckoo-root"
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
    testSupportJS,
    testSupportJVM
  )

// Core ==================================================

lazy val core = (crossProject.crossType(CrossType.Pure) in file("core"))
  .settings(
    name := "core",
    moduleName := "quckoo-core",
    buildInfoPackage := "io.quckoo",
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion),
    buildInfoObject := "Info"
  )
  .enablePlugins(BuildInfoPlugin, AutomateHeaderPlugin)
  .settings(commonSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(publishSettings: _*)
  .settings(Dependencies.core: _*)
  .jsSettings(commonJsSettings: _*)

lazy val coreJS = core.js
lazy val coreJVM = core.jvm

// API ==================================================

lazy val api = (crossProject.crossType(CrossType.Pure) in file("api"))
  .settings(
    name := "api",
    moduleName := "quckoo-api"
  )
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(publishSettings: _*)
  .settings(Dependencies.api: _*)
  .jsSettings(commonJsSettings: _*)
  .dependsOn(core)

lazy val apiJS = api.js
lazy val apiJVM = api.jvm

// Client ==================================================

lazy val client = (crossProject in file("client"))
  .settings(
    name := "client",
    moduleName := "quckoo-client",
    requiresDOM := true
  )
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(publishSettings: _*)
  .settings(Dependencies.client: _*)
  .jsSettings(commonJsSettings: _*)
  .jsSettings(Dependencies.clientJS: _*)
  .jvmSettings(Dependencies.clientJVM: _*)
  .dependsOn(api, testSupport % Test)

lazy val clientJS = client.js
lazy val clientJVM = client.jvm

// Console ==================================================

lazy val console = (project in file("console"))
  .enablePlugins(AutomateHeaderPlugin, ScalaJSPlugin, ScalaJSWeb)
  .settings(
    name := "console",
    moduleName := "quckoo-console",
    requiresDOM := true,
    persistLauncher in Compile := true
  )
  .settings(commonSettings: _*)
  .settings(commonJsSettings: _*)
  .settings(publishSettings: _*)
  .settings(Dependencies.console: _*)
  .dependsOn(clientJS)

// Cluster ==================================================

lazy val cluster = (project in file("cluster"))
  .settings(name := "quckoo-cluster")
  .settings(noPublishSettings)
  .settings(scoverageSettings)
  .aggregate(clusterShared, clusterMaster, clusterWorker)

lazy val clusterShared = (project in file("cluster/shared"))
  .settings(
    name := "cluster-shared",
    moduleName := "quckoo-cluster-shared"
  )
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(publishSettings: _*)
  .settings(Dependencies.clusterShared)
  .dependsOn(apiJVM, testSupportJVM % Test)

lazy val clusterMaster = (project in file("cluster/master"))
  .enablePlugins(AutomateHeaderPlugin, SbtSass, SbtTwirl, JavaServerAppPackaging, DockerPlugin)
  .configs(MultiJvm)
  .settings(
    name := "cluster-master",
    moduleName := "quckoo-cluster-master",
    scalaJSProjects := Seq(console),
    baseDirectory in reStart := file("cluster/master/target"),
    compile in Compile <<= (compile in Compile) dependsOn scalaJSPipeline,
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value,
    pipelineStages in Assets := Seq(scalaJSPipeline)
  )
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(Revolver.settings: _*)
  .settings(Dependencies.clusterMaster)
  .settings(MultiNode.settings)
  .settings(Packaging.masterSettings: _*)
  .dependsOn(clusterShared, testSupportJVM % Test)

lazy val clusterWorker = (project in file("cluster/worker"))
  .enablePlugins(AutomateHeaderPlugin, JavaServerAppPackaging, DockerPlugin)
  .settings(
    name := "cluster-worker",
    moduleName := "quckoo-cluster-worker"
  )
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(Revolver.settings: _*)
  .settings(Dependencies.clusterWorker)
  .settings(Packaging.workerSettings: _*)
  .settings(
    baseDirectory in reStart := file("cluster/worker/target")
  )
  .dependsOn(clusterShared, testSupportJVM % Test)

// Test Support Utils ========================================

lazy val testSupport = (crossProject in file("test-support"))
  .settings(
    name := "test-support",
    moduleName := "quckoo-test-support"
  )
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(Dependencies.testSupport: _*)
  .jsSettings(commonJsSettings: _*)
  .jvmSettings(Dependencies.testSupportJVM: _*)

lazy val testSupportJS = testSupport.js
lazy val testSupportJVM = testSupport.jvm

// Examples ==================================================

lazy val examples = (project in file("examples"))
  .settings(moduleName := "quckoo-examples")
  .aggregate(exampleJobs, exampleProducers)
  .settings(noPublishSettings)

lazy val exampleJobs = (project in file("examples/jobs"))
  .settings(
    name := "example-jobs",
    moduleName := "quckoo-example-jobs"
  )
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(Dependencies.exampleJobs)
  .dependsOn(coreJVM)

lazy val exampleProducers = (project in file("examples/producers"))
  .enablePlugins(AutomateHeaderPlugin, JavaAppPackaging, DockerPlugin)
  .settings(
    name := "example-producers",
    moduleName := "quckoo-example-producers"
  )
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(Revolver.settings: _*)
  .settings(Dependencies.exampleProducers)
  .settings(Packaging.exampleProducersSettings: _*)
  .dependsOn(clientJVM, exampleJobs)

// Command aliases ==================================================

addCommandAlias("testJS",
                ";coreJS/test;apiJS/test;clientJS/test;consoleApp/test")
addCommandAlias(
  "testJVM",
  ";coreJVM/test;apiJVM/test;clientJVM/test;cluster/test;examples/test")
