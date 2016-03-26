import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._
import Keys._

object Dependencies {

  object version {

    // Logging -------

    val slf4s = "1.7.12"
    val log4j = "2.3"

    // Testing --------

    val scalaTest = "3.0.0-M15"
    val scalaMock = "3.2.2"
    val mockito   = "1.10.19"

    // Akka ----------

    val akka = "2.4.2"
    val kryo = "0.4.0"

    // ScalaJS -------

    val scalaJsReact = "0.10.4"
    val scalaCss = "0.3.1"

    val diode = "0.5.1-SNAPSHOT"

    val upickle = "0.3.8"
    val scalatags = "0.4.6"

    val reactJs = "0.14.3"

    // Other utils ---

    val scopt = "3.3.0"
    val monocle = "1.1.1"
    val scalaz = "7.1.3"
  }

  // Common library definitions

  object libs {

    val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.5"

    val ivy = "org.apache.ivy" % "ivy" % "2.4.0"

    object Akka {
      val actor           = "com.typesafe.akka" %% "akka-actor"             % version.akka
      val slf4j           = "com.typesafe.akka" %% "akka-slf4j"             % version.akka
      val clusterTools    = "com.typesafe.akka" %% "akka-cluster-tools"     % version.akka
      val clusterMetrics  = "com.typesafe.akka" %% "akka-cluster-metrics"   % version.akka
      val sharding        = "com.typesafe.akka" %% "akka-cluster-sharding"  % version.akka
      val http            = "com.typesafe.akka" %% "akka-http-experimental" % version.akka
      val httpTestkit     = "com.typesafe.akka" %% "akka-http-testkit"      % version.akka % Test
      val httpUpickle     = "de.heikoseeberger" %% "akka-http-upickle"      % "1.5.2"
      val sse             = "de.heikoseeberger" %% "akka-sse"               % "1.6.1"
      val distributedData = "com.typesafe.akka" %% "akka-distributed-data-experimental" % version.akka

      object persistence {
        val core      = "com.typesafe.akka"   %% "akka-persistence"              % version.akka
        val query     = "com.typesafe.akka"   %% "akka-persistence-query-experimental" % version.akka
        val cassandra = "com.typesafe.akka"   %% "akka-persistence-cassandra"    % "0.11"
        val memory    = "com.github.dnvriend" %% "akka-persistence-inmemory"     % "1.2.11" % Test
      }

      val multiNodeTestKit = "com.typesafe.akka" %% "akka-multi-node-testkit" % version.akka
      val testKit          = "com.typesafe.akka" %% "akka-testkit"            % version.akka % Test

      val kryoSerialization = "com.github.romix.akka" %% "akka-kryo-serialization" % version.kryo
    }

    object Log4j {
      val api       = "org.apache.logging.log4j"  % "log4j-api"        % version.log4j
      val core      = "org.apache.logging.log4j"  % "log4j-core"       % version.log4j
      val slf4jImpl = "org.apache.logging.log4j"  % "log4j-slf4j-impl" % version.log4j % Runtime
    }
    val slf4s = "org.slf4s" %% "slf4s-api" % version.slf4s

    val scopt = "com.github.scopt" %% "scopt" % version.scopt

    val scalaTest = "org.scalatest" %% "scalatest"                   % version.scalaTest % Test
    val scalaMock = "org.scalamock" %% "scalamock-scalatest-support" % version.scalaMock % Test
    val mockito   = "org.mockito"    % "mockito-core"                % version.mockito   % Test

    val scalaz = "org.scalaz" %% "scalaz-core" % version.scalaz

    object Monocle {
      val core    = "com.github.julien-truffaut"  %%  "monocle-core"    % version.monocle
      val `macro` = "com.github.julien-truffaut"  %%  "monocle-macro"   % version.monocle
    }

  }

  object compiler {
    val macroParadise = "org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full
  }

  // Common module ===============================

  lazy val common = Def.settings {
    libraryDependencies ++= Seq(
      compilerPlugin(Dependencies.compiler.macroParadise),

      "com.lihaoyi"   %%% "upickle"   % version.upickle,
      "org.scalatest" %%% "scalatest" % version.scalaTest % Test
    )
  }
  lazy val commonJS = Def.settings {
    libraryDependencies ++= Seq(
      "io.github.widok"                  %%% "scala-js-momentjs" % "0.1.4",
      "com.github.japgolly.fork.scalaz"  %%% "scalaz-core"       % version.scalaz,
      "com.github.japgolly.fork.monocle" %%% "monocle-core"      % version.monocle,
      "com.github.japgolly.fork.monocle" %%% "monocle-macro"     % version.monocle
    )
  }
  lazy val commonJVM = Def.settings {
    import libs._
    libraryDependencies ++= Seq(scalaz, Monocle.core, Monocle.`macro`)
  }

  // API module ===============================

  lazy val api = Def.settings {
    addCompilerPlugin(compiler.macroParadise)
  }

  // Client module ===============================

  lazy val client = Def.settings {
    libraryDependencies ++= Seq(
      compilerPlugin(Dependencies.compiler.macroParadise),

      "org.scalatest" %%% "scalatest" % version.scalaTest % Test
    )
  }

  lazy val clientJS = Def.settings {
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.0"
    )
  }

  lazy val clientJVM = Def.settings {
    import libs._
    libraryDependencies ++= Seq(
      slf4s, Log4j.api, Log4j.core, Log4j.slf4jImpl,
      Akka.actor, Akka.slf4j, Akka.clusterTools, Akka.clusterMetrics, Akka.testKit,
      Akka.kryoSerialization, scalaTest, scalaMock
    )
  }

  // Console module ===============================

  lazy val consoleApp = Def.settings(
    libraryDependencies ++= Seq(
      compilerPlugin(Dependencies.compiler.macroParadise),

      "com.lihaoyi"      %%% "scalatags"      % version.scalatags,
      "org.scalatest"    %%% "scalatest"      % version.scalaTest % Test,
      "biz.enef"         %%% "slogging"       % "0.3",
      "me.chrons"        %%% "diode"          % version.diode,
      "me.chrons"        %%% "diode-react"    % version.diode,
      "org.monifu"       %%% "monifu"         % "1.0",
      "be.doeraene"      %%% "scalajs-jquery" % "0.9.0",
      "org.singlespaced" %%% "scalajs-d3"     % "0.3.1",

      "com.github.japgolly.scalajs-react" %%% "core"         % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "extra"        % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-scalaz71" % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-monocle"  % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "test"         % version.scalaJsReact % Test,
      "com.github.japgolly.scalacss"      %%% "core"         % version.scalaCss,
      "com.github.japgolly.scalacss"      %%% "ext-react"    % version.scalaCss
    ),
    jsDependencies ++= Seq(
      "org.webjars.bower" % "react" % version.reactJs / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
      "org.webjars.bower" % "react" % version.reactJs / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM",
      "org.webjars.bower" % "react" % version.reactJs % Test / "react-with-addons.js" commonJSName "React",

      "org.webjars" % "jquery"    % "1.11.1" / "jquery.js"    minified "jquery.min.js",
      "org.webjars" % "bootstrap" % "3.3.2"  / "bootstrap.js" minified "bootstrap.min.js" dependsOn "jquery.js"
    )
  )
  lazy val consoleResources = Def.settings {
    libraryDependencies ++= Seq(
      "org.webjars" % "bootstrap-sass" % "3.3.1",
      "org.webjars" % "font-awesome"   % "4.5.0"
    )
  }

  // Cluster modules ===============================

  lazy val clusterShared = Def.settings {
    import libs._
    libraryDependencies ++= Seq(
      slf4s, Log4j.api, Log4j.core, Log4j.slf4jImpl,
      Akka.actor, Akka.slf4j, Akka.clusterTools, Akka.clusterMetrics, Akka.testKit,
      Akka.kryoSerialization, ivy, scalaXml, mockito, scalaTest, scalaMock
    )
  }
  lazy val clusterMaster = Def.settings {
    import libs._
    libraryDependencies ++= Seq(Log4j.slf4jImpl,
      Akka.sharding, Akka.http, Akka.httpTestkit, Akka.httpUpickle, Akka.sse,
      Akka.distributedData, Akka.persistence.core, Akka.persistence.cassandra,
      Akka.persistence.query, Akka.persistence.memory, scopt, scalaTest, scalaMock
    )
  }
  lazy val clusterWorker = Def.settings {
    import libs._
    libraryDependencies ++= Seq(Log4j.slf4jImpl,
      Akka.testKit, scopt, scalaTest, scalaMock
    )
  }

  // Examples modules ===============================

  lazy val exampleJobs = Def.settings {
    import libs._
    libraryDependencies ++= Seq(slf4s, Log4j.api, Log4j.core, Log4j.slf4jImpl)
  }
  lazy val exampleProducers = Def.settings {
    import libs._
    libraryDependencies ++= Seq(Akka.testKit, scopt)
  }

}
