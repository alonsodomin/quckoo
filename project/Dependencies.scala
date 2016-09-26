import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._
import Keys._

object Dependencies {

  object version {

    // Logging -------

    val slf4s = "1.7.12"
    val log4j = "2.6.2"

    // Testing --------

    val scalaTest  = "3.0.0"
    val scalaCheck = "1.13.2"
    val scalaMock  = "3.2.2"
    val mockito    = "1.10.19"
    val mockserver = "3.10.4"

    // Akka ----------

    object akka {
      val main = "2.4.10"
      val kryo = "0.4.1"

      // http extensions
      val json = "1.9.0"
      val sse  = "1.10.0"

      // persistence plugins
      val cassandra = "0.18"
      val inmemory  = "1.3.8"
    }

    // ScalaJS -------

    val scalaJsReact    = "0.11.2"
    val scalaJsDom      = "0.9.1"
    val scalaJsJQuery   = "0.9.0"

    val testState = "2.1.0"
    val scalaCss  = "0.5.0"
    val scalaTime = "2.0.0-M3"

    val diode = "1.0.0"

    val upickle   = "0.4.1"
    val scalatags = "0.6.0"

    // Other utils ---

    val scopt      = "3.5.0"
    val slogging   = "0.5.0"
    val monocle    = "1.2.2"
    val scalaz     = "7.2.6"
    val monix      = "2.0.1"
    val cron4s     = "0.2.0"
    val enumeratum = "1.4.14"

    // JavaScript Libraries

    val jquery = "1.11.1"
    val bootstrap = "3.3.2"
    val bootstrapNotifiy = "3.1.3"
    val reactJs = "15.3.2"
  }

  // Common library definitions

  object libs {

    val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.5"

    val ivy = "org.apache.ivy" % "ivy" % "2.4.0"

    object Akka {
      val actor           = "com.typesafe.akka" %% "akka-actor"             % version.akka.main
      val slf4j           = "com.typesafe.akka" %% "akka-slf4j"             % version.akka.main
      val clusterTools    = "com.typesafe.akka" %% "akka-cluster-tools"     % version.akka.main
      val clusterMetrics  = "com.typesafe.akka" %% "akka-cluster-metrics"   % version.akka.main
      val sharding        = "com.typesafe.akka" %% "akka-cluster-sharding"  % version.akka.main
      val http            = "com.typesafe.akka" %% "akka-http-experimental" % version.akka.main
      val distributedData = "com.typesafe.akka" %% "akka-distributed-data-experimental" % version.akka.main
      val httpTestkit     = "com.typesafe.akka" %% "akka-http-testkit"      % version.akka.main % Test
      val httpUpickle     = "de.heikoseeberger" %% "akka-http-upickle"      % version.akka.json
      val sse             = "de.heikoseeberger" %% "akka-sse"               % version.akka.sse

      object persistence {
        val core      = "com.typesafe.akka"   %% "akka-persistence"              % version.akka.main
        val query     = "com.typesafe.akka"   %% "akka-persistence-query-experimental" % version.akka.main
        val cassandra = "com.typesafe.akka"   %% "akka-persistence-cassandra"    % version.akka.cassandra
        val memory    = "com.github.dnvriend" %% "akka-persistence-inmemory"     % version.akka.inmemory % Test
      }

      val multiNodeTestKit = "com.typesafe.akka" %% "akka-multi-node-testkit" % version.akka.main
      val testKit          = "com.typesafe.akka" %% "akka-testkit"            % version.akka.main % Test

      val kryoSerialization = "com.github.romix.akka" %% "akka-kryo-serialization" % version.akka.kryo
    }

    object Log4j {
      val api       = "org.apache.logging.log4j"  % "log4j-api"        % version.log4j
      val core      = "org.apache.logging.log4j"  % "log4j-core"       % version.log4j
      val slf4jImpl = "org.apache.logging.log4j"  % "log4j-slf4j-impl" % version.log4j % Runtime
    }
    val slf4s = "org.slf4s" %% "slf4s-api" % version.slf4s

    val scopt = "com.github.scopt" %% "scopt" % version.scopt

    val authenticatJwt = "com.jason-goodwin" %% "authentikat-jwt" % "0.4.1"

    val scalaTest  = "org.scalatest"   %% "scalatest"              % version.scalaTest
    val scalaMock  = "org.scalamock"   %% "scalamock-core"         % version.scalaMock
    val mockito    = "org.mockito"      % "mockito-core"           % version.mockito
    val mockserver = "org.mock-server"  % "mockserver-netty"       % version.mockserver
  }

  object compiler {
    val macroParadise = "org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full
  }

  // Core module ===============================

  lazy val core = Def.settings {
    libraryDependencies ++= Seq(
      compilerPlugin(Dependencies.compiler.macroParadise),

      "com.lihaoyi"    %%% "upickle"            % version.upickle,
      "com.beachape"   %%% "enumeratum"         % version.enumeratum,
      "com.beachape"   %%% "enumeratum-upickle" % version.enumeratum,
      "org.scalaz"     %%% "scalaz-core"        % version.scalaz,
      "io.github.soc"  %%% "scala-java-time"    % version.scalaTime,
      "org.scalatest"  %%% "scalatest"          % version.scalaTest  % Test,

      "com.github.julien-truffaut" %%% "monocle-core"  % version.monocle,
      "com.github.julien-truffaut" %%% "monocle-macro" % version.monocle,

      "com.github.alonsodomin.cron4s" %%% "cron4s-core" % version.cron4s
    )
  }

  // API module ===============================

  lazy val api = Def.settings(
    addCompilerPlugin(compiler.macroParadise),
    libraryDependencies ++= Seq(
      "me.chrons"      %%% "diode"           % version.diode,
      "io.monix"       %%% "monix-reactive"  % version.monix,
      "io.monix"       %%% "monix-scalaz-72" % version.monix,
      "org.scalatest"  %%% "scalatest"       % version.scalaTest  % Test,
      "org.scalacheck" %%% "scalacheck"      % version.scalaCheck % Test
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

  lazy val clientJS = Def.settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % version.scalaJsDom
    )
  )

  lazy val clientJVM = Def.settings {
    import libs._
    libraryDependencies ++= Seq(
      slf4s, Log4j.api, Log4j.core, Log4j.slf4jImpl,
      Akka.actor, Akka.slf4j, Akka.clusterTools, Akka.clusterMetrics, Akka.testKit,
      Akka.kryoSerialization, Akka.http, Akka.sse,
      mockserver % Test
    )
  }

  // Console module ===============================

  lazy val consoleApp = Def.settings(
    libraryDependencies ++= Seq(
      compilerPlugin(Dependencies.compiler.macroParadise),

      "com.lihaoyi"      %%% "scalatags"      % version.scalatags,
      "org.scalatest"    %%% "scalatest"      % version.scalaTest % Test,
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
    )
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
      Akka.kryoSerialization, ivy, scalaXml
    )
  }
  lazy val clusterMaster = Def.settings {
    import libs._
    libraryDependencies ++= Seq(Log4j.slf4jImpl,
      Akka.sharding, Akka.http, Akka.httpTestkit, Akka.httpUpickle, Akka.sse,
      Akka.distributedData, Akka.persistence.core, Akka.persistence.cassandra,
      Akka.persistence.query, Akka.persistence.memory, scopt,
      authenticatJwt
    )
  }
  lazy val clusterWorker = Def.settings {
    import libs._
    libraryDependencies ++= Seq(Log4j.slf4jImpl,
      Akka.testKit, scopt
    )
  }

  // Support modules ================================

  lazy val testSupportJVM = Def.settings {
    import libs._
    libraryDependencies ++= Seq(
      mockito, scalaTest, scalaMock
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
