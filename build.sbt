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
  settings(addCompilerPlugin(Dependencies.compiler.macroParadise)).
  settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi"    %%% "upickle" % "0.3.6",
      "org.scalatest"  %%% "scalatest" % Dependencies.version.scalaTest % Test
    )
  ).
  jsSettings(
    libraryDependencies ++= {
      import Dependencies.version._

      Seq(
        "io.github.widok" %%% "scala-js-momentjs" % "0.1.4",
        "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % scalaz,
        "com.github.japgolly.fork.monocle" %%% "monocle-core" % monocle,
        "com.github.japgolly.fork.monocle" %%% "monocle-macro" % monocle
      )
    }
  ).jvmSettings(
    libraryDependencies ++= {
      import Dependencies.libs._

      Seq(scalaz, Monocle.core, Monocle.`macro`)
    }
  )

lazy val commonJS = common.js
lazy val commonJVM = common.jvm

lazy val client = (project in file("client")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.client
  ).
  dependsOn(commonJVM)

// Console ==================================================

lazy val consoleRoot = (project in file("console")).
  settings(moduleName := "console").
  settings(noPublishSettings).
  aggregate(consoleJS, consoleJVM, consoleResources)

lazy val console = (crossProject in file("console")).
  settings(commonSettings: _*).
  settings(addCompilerPlugin(Dependencies.compiler.macroParadise)).
  settings(
    name := "console",
    coverageEnabled := false,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.4.6",
      "com.lihaoyi" %%% "upickle" % "0.3.8",
      "org.scalatest"  %%% "scalatest" % Dependencies.version.scalaTest % Test
    )
  ).
  jsSettings(
    libraryDependencies ++= {
      import Dependencies.version._

      Seq(
        "biz.enef" %%% "slogging" % "0.3",
        "me.chrons" %%% "diode" % "0.5.0",
        "me.chrons" %%% "diode-react" % "0.5.0",
        "org.monifu" %%% "monifu" % "1.0",
        "be.doeraene" %%% "scalajs-jquery" % "0.9.0",
        "org.singlespaced" %%% "scalajs-d3" % "0.3.1",
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
        "org.webjars.bower" % "react" % reactJs % "test" / "react-with-addons.js" commonJSName "React",
        "org.webjars" % "jquery" % "1.11.1" / "jquery.js" minified "jquery.min.js",
        "org.webjars" % "bootstrap" % "3.3.2" / "bootstrap.js" minified "bootstrap.min.js" dependsOn "jquery.js"
      )
    },
    requiresDOM := true,
    coverageEnabled := false,
    persistLauncher in Compile := true,
    persistLauncher in Test := false,
    scalaJSStage in Test := FastOptStage,
    jsEnv in Test := new PhantomJS2Env(scalaJSPhantomJSClassLoader.value)
  )
  .jvmSettings(
    libraryDependencies ++= {
      import Dependencies.libs._

      Seq(Akka.http, Akka.httpTestkit, Akka.httpUpickle, Akka.sse)
    }
  ).
  dependsOn(common)

lazy val consoleJS = console.js
lazy val consoleJVM = console.jvm

lazy val consoleResources = (project in file("console/resources")).
  aggregate(consoleJS).
  enablePlugins(SbtSass).
  settings(commonSettings: _*).
  settings(
    name := "console-resources",
    libraryDependencies ++= Seq(
      "org.webjars" % "bootstrap-sass" % "3.3.1",
      "org.webjars" % "font-awesome"   % "4.5.0"
    ),
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
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.cluster
  ).
  dependsOn(commonJVM)

lazy val master = MultiNode(Project("cluster-master", file("cluster/master"))).
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.master,
    reStart <<= reStart dependsOn ((packageBin in Compile) in consoleResources)
  ).
  enablePlugins(JavaServerAppPackaging, DockerPlugin).
  settings(Packaging.universalServerSettings: _*).
  settings(Packaging.masterDockerSettings: _*).
  dependsOn(clusterShared, consoleResources, consoleJVM)

lazy val worker = Project("cluster-worker", file("cluster/worker")).
  settings(commonSettings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.module.worker
  ).
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
  enablePlugins(JavaAppPackaging, DockerPlugin).
  settings(Packaging.universalSettings: _*).
  settings(Packaging.dockerSettings: _*).
  dependsOn(commonJVM, exampleJobs, client)

// Command aliases ==================================================

addCommandAlias("testJS", ";commonJS/test;consoleJS/test")
addCommandAlias("testJVM", ";commonJVM/test;client/test;consoleJVM/test;cluster/test;examples/test")