import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._
import Keys._

object Dependencies {

  object version {

    // Logging -------

    val slf4s = "1.7.12"
    val log4j = "2.6.2"

    // Testing --------

    val scalaTest  = "3.0.0-M15"
    val scalaCheck = "1.13.2"
    val scalaMock  = "3.2.2"
    val mockito    = "1.10.19"

    // Akka ----------

    val akka = "2.4.8"
    val kryo = "0.4.1"

    // ScalaJS -------

    val scalaJsReact    = "0.11.1"
    val scalaJsDom      = "0.9.1"
    val scalaJsJQuery   = "0.9.0"
    val scalaJSMomentJS = "0.1.5"

    val testState = "2.0.0"
    val scalaCss  = "0.4.1"

    val diode = "1.0.0"

    val upickle   = "0.4.1"
    val utest     = "0.4.3"
    val scalatags = "0.5.5"

    // Other utils ---

    val scopt    = "3.5.0"
    val slogging = "0.5.0"
    val monocle  = "1.2.2"
    val scalaz   = "7.2.4"
    val monix    = "2.0-RC9"

    // JavaScript Libraries

    val jquery = "1.11.1"
    val bootstrap = "3.3.2"
    val bootstrapNotifiy = "3.1.3"
    val reactJs = "15.2.1"
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
      val httpUpickle     = "de.heikoseeberger" %% "akka-http-upickle"      % "1.8.0"
      val sse             = "de.heikoseeberger" %% "akka-sse"               % "1.8.1"
      val distributedData = "com.typesafe.akka" %% "akka-distributed-data-experimental" % version.akka

      object persistence {
        val core      = "com.typesafe.akka"   %% "akka-persistence"              % version.akka
        val query     = "com.typesafe.akka"   %% "akka-persistence-query-experimental" % version.akka
        val cassandra = "com.typesafe.akka"   %% "akka-persistence-cassandra"    % "0.17"
        val memory    = "com.github.dnvriend" %% "akka-persistence-inmemory"     % "1.3.2" % Test
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

    val authenticatJwt = "com.jason-goodwin" %% "authentikat-jwt" % "0.4.1"

    val scalaCheck = "org.scalacheck" %% "scalacheck"                  % version.scalaCheck % Test
    val scalaTest  = "org.scalatest"  %% "scalatest"                   % version.scalaTest  % Test
    val scalaMock  = "org.scalamock"  %% "scalamock-scalatest-support" % version.scalaMock  % Test
    val mockito    = "org.mockito"     % "mockito-core"                % version.mockito    % Test
  }

  object compiler {
    val macroParadise = "org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full
  }

  // Core module ===============================

  lazy val core = Def.settings {
    libraryDependencies ++= Seq(
      compilerPlugin(Dependencies.compiler.macroParadise),

      "com.lihaoyi"    %%% "upickle"     % version.upickle,
      "org.scalaz"     %%% "scalaz-core" % version.scalaz,
      "org.scalatest"  %%% "scalatest"   % version.scalaTest  % Test,
      "org.scalacheck" %%% "scalacheck"  % version.scalaCheck % Test,

      "com.github.julien-truffaut" %%% "monocle-core"  % version.monocle,
      "com.github.julien-truffaut" %%% "monocle-macro" % version.monocle
    )
  }
  lazy val coreJS = Def.settings {
    libraryDependencies += "io.github.widok" %%% "scala-js-momentjs" % version.scalaJSMomentJS
  }
  //lazy val coreJVM = Def.settings { }

  // API module ===============================

  lazy val api = Def.settings(
    addCompilerPlugin(compiler.macroParadise),
    libraryDependencies ++= Seq(
      "me.chrons" %%% "diode"          % version.diode,
      "io.monix"  %%% "monix-reactive" % version.monix
    )
  )

  // Client module ===============================

  lazy val client = Def.settings {
    libraryDependencies ++= Seq(
      compilerPlugin(Dependencies.compiler.macroParadise),

      "biz.enef"      %%% "slogging"  % version.slogging,
      "org.scalatest" %%% "scalatest" % version.scalaTest % Test
    )
  }

  lazy val clientJS = Def.settings {
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % version.scalaJsDom
    )
  }

  lazy val clientJVM = Def.settings {
    import libs._
    libraryDependencies ++= Seq(
      slf4s, Log4j.api, Log4j.core, Log4j.slf4jImpl,
      Akka.actor, Akka.slf4j, Akka.clusterTools, Akka.clusterMetrics, Akka.testKit,
      Akka.kryoSerialization, scalaTest
    )
  }

  // Console module ===============================

  lazy val consoleApp = Def.settings(
    libraryDependencies ++= Seq(
      compilerPlugin(Dependencies.compiler.macroParadise),

      "com.lihaoyi"      %%% "scalatags"      % version.scalatags,
      "org.scalatest"    %%% "scalatest"      % version.scalaTest % Test,
      "com.lihaoyi"      %%% "utest"          % version.utest     % Test,
      "me.chrons"        %%% "diode-react"    % version.diode,
      "be.doeraene"      %%% "scalajs-jquery" % version.scalaJsJQuery,

      "com.github.japgolly.scalajs-react" %%% "core"         % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "extra"        % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-scalaz72" % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-monocle"  % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "test"         % version.scalaJsReact % Test,
      "com.github.japgolly.scalacss"      %%% "core"         % version.scalaCss,
      "com.github.japgolly.scalacss"      %%% "ext-react"    % version.scalaCss,

      "com.github.japgolly.test-state" %%% "core"              % version.testState % Test,
      "com.github.japgolly.test-state" %%% "dom-zipper"        % version.testState % Test,
      "com.github.japgolly.test-state" %%% "dom-zipper-sizzle" % version.testState % Test,
      "com.github.japgolly.test-state" %%% "ext-scalajs-react" % version.testState % Test,
      "com.github.japgolly.test-state" %%% "ext-scalaz"        % version.testState % Test
    ),
    jsDependencies ++= Seq(
      // ReactJS
      "org.webjars.bower" % "react" % version.reactJs
        /        "react-with-addons.js"
        minified "react-with-addons.min.js"
        commonJSName "React",
      "org.webjars.bower" % "react" % version.reactJs
        /         "react-dom.js"
        minified  "react-dom.min.js"
        dependsOn "react-with-addons.js"
        commonJSName "ReactDOM",
      "org.webjars.bower" % "react" % version.reactJs
        /         "react-dom-server.js"
        minified  "react-dom-server.min.js"
        dependsOn "react-dom.js"
        commonJSName "ReactDOMServer",
      "org.webjars.bower" % "react" % version.reactJs % Test
        /            "react-with-addons.js"
        commonJSName "React",

      // JQuery & Bootstrap
      "org.webjars" % "jquery"    % version.jquery
        /        s"${version.jquery}/jquery.js"
        minified "jquery.min.js",
      "org.webjars" % "bootstrap" % version.bootstrap
        /         "bootstrap.js"
        minified  "bootstrap.min.js"
        dependsOn s"${version.jquery}/jquery.js",
      "org.webjars" % "bootstrap-notify" % version.bootstrapNotifiy
        /         "bootstrap-notify.js"
        minified  "bootstrap-notify.min.js"
        dependsOn (s"${version.jquery}/jquery.js", "bootstrap.js")
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )
  lazy val consoleResources = Def.settings {
    libraryDependencies ++= Seq(
      "org.webjars"       % "bootstrap-sass"  % "3.3.1",
      "org.webjars"       % "font-awesome"    % "4.5.0",
      "org.webjars.bower" % "animatewithsass" % "3.2.2"
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
      Akka.persistence.query, Akka.persistence.memory, scopt, scalaTest,
      authenticatJwt
    )
  }
  lazy val clusterWorker = Def.settings {
    import libs._
    libraryDependencies ++= Seq(Log4j.slf4jImpl,
      Akka.testKit, scopt, scalaTest
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
