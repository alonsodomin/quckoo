import sbt._
import Keys._

import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Dependencies {

  object version {

    // Logging -------

    val slogging = "0.6.0"
    val log4j    = "2.9.1"
    val slf4j    = "1.7.25"

    // Testing --------

    val scalaTest  = "3.0.4"
    val scalaCheck = "1.13.5"
    val scalaMock  = "3.6.0"
    val mockserver = "3.10.8"
    val discipline = "0.8"

    // Akka ----------

    object akka {
      val main = "2.5.6"
      val kryo = "0.9.2"

      val constructr = "0.7.0"

      object http {
        val main = "10.0.10"

        // http extensions
        val json = "1.18.1"
        val sse  = "3.0.0"
      }

      // persistence plugins
      val cassandra = "0.58"
      val inmemory  = "2.5.1.1"
    }

    // Monitoring ----

    object kamon {
      val core = "0.6.7"
      val http = "0.6.8"
    }

    // ScalaJS -------

    val scalaJsReact    = "1.1.1"
    val scalaJsDom      = "0.9.3"
    val scalaJsJQuery   = "0.9.2"
    val scalaJSScripts  = "1.1.1"
    val testState       = "2.1.2"

    // Other utils ---

    val arm         = "2.0"
    val betterfiles = "3.2.0"
    val diode       = "1.1.2"
    val cats        = "1.0.0-MF"
    val catsEffect  = "0.4"
    val circe       = "0.9.0-M1"
    val cron4s      = "0.4.2"
    val enumeratum  = "1.5.12"
    val enumCirce   = "1.5.14"
    val ivy         = "2.4.0"
    val monix       = "3.0.0-M1"
    val monocle     = "1.4.0"
    val pureconfig  = "0.8.0"
    val refined     = "0.8.4"
    val scalaCss    = "0.5.3"
    val scalafmt    = "1.3.0"
    val scalaTime   = "2.0.0-M12"
    val scopt       = "3.7.0"
    val xml         = "1.0.6"

    // JavaScript Libraries

    val animate          = "3.2.2"
    val jquery           = "2.2.4"
    val bootstrap        = "3.3.7"
    val bootstrapNotifiy = "3.1.3"
    val reactJs          = "15.6.1"
    val sparkMD5         = "3.0.0"
    val codemirror       = "5.24.2"
  }

  // Common library definitions

  object libs {

    val scalaArm = "com.jsuereth"           %% "scala-arm" % version.arm
    val scalaXml = "org.scala-lang.modules" %% "scala-xml" % version.xml

    val ivy = "org.apache.ivy" % "ivy" % version.ivy

    object Akka {
      val actor           = "com.typesafe.akka" %% "akka-actor"             % version.akka.main
      val slf4j           = "com.typesafe.akka" %% "akka-slf4j"             % version.akka.main
      val clusterTools    = "com.typesafe.akka" %% "akka-cluster-tools"     % version.akka.main
      val clusterMetrics  = "com.typesafe.akka" %% "akka-cluster-metrics"   % version.akka.main
      val sharding        = "com.typesafe.akka" %% "akka-cluster-sharding"  % version.akka.main
      val distributedData = "com.typesafe.akka" %% "akka-distributed-data"  % version.akka.main

      object http {
        val main    = "com.typesafe.akka" %% "akka-http"         % version.akka.http.main
        val testkit = "com.typesafe.akka" %% "akka-http-testkit" % version.akka.http.main
        val circe   = "de.heikoseeberger" %% "akka-http-circe"   % version.akka.http.json
        val sse     = "de.heikoseeberger" %% "akka-sse"          % version.akka.http.sse
      }

      object persistence {
        val core      = "com.typesafe.akka"   %% "akka-persistence"           % version.akka.main
        val query     = "com.typesafe.akka"   %% "akka-persistence-query"     % version.akka.main
        val cassandra = "com.typesafe.akka"   %% "akka-persistence-cassandra" % version.akka.cassandra
        val memory    = "com.github.dnvriend" %% "akka-persistence-inmemory"  % version.akka.inmemory % Test
      }

      val multiNodeTestKit = "com.typesafe.akka" %% "akka-multi-node-testkit" % version.akka.main
      val testKit          = "com.typesafe.akka" %% "akka-testkit"            % version.akka.main % Test

      val kryo       = "com.twitter" %% "chill-akka"                     % version.akka.kryo
      val constructr = "com.tecsisa" %% "constructr-coordination-consul" % version.akka.constructr % Runtime
    }

    object Log4j {
      val api       = "org.apache.logging.log4j"  % "log4j-api"        % version.log4j
      val core      = "org.apache.logging.log4j"  % "log4j-core"       % version.log4j
      val slf4jImpl = "org.apache.logging.log4j"  % "log4j-slf4j-impl" % version.log4j

      val All = Seq(api, core, slf4jImpl)
    }

    object Kamon {
      val core       = "io.kamon" %% "kamon-core"            % version.kamon.core
      val akka       = "io.kamon" %% "kamon-akka-remote-2.4" % version.kamon.core
      val http       = "io.kamon" %% "kamon-akka-http"       % version.kamon.http
      val scala      = "io.kamon" %% "kamon-scala"           % version.kamon.core
      val sysmetrics = "io.kamon" %% "kamon-system-metrics"  % version.kamon.core
      val statsd     = "io.kamon" %% "kamon-statsd"          % version.kamon.core
    }

    val slf4j          = "org.slf4j" % "slf4j-api"      % version.slf4j
    val slogging_slf4j = "biz.enef" %% "slogging-slf4j" % version.slogging

    val scopt = "com.github.scopt" %% "scopt" % version.scopt

    val authenticatJwt = "com.jason-goodwin"     %% "authentikat-jwt" % "0.4.5"
    val pureconfig     = "com.github.pureconfig" %% "pureconfig" % version.pureconfig

    val scalaTest  = "org.scalatest"   %% "scalatest"                   % version.scalaTest
    val scalaMock  = "org.scalamock"   %% "scalamock-scalatest-support" % version.scalaMock
    val mockserver = "org.mock-server"  % "mockserver-netty"            % version.mockserver excludeAll(
      ExclusionRule(organization = "org.slf4j"),
      ExclusionRule(organization = "ch.qos.logback"),
      ExclusionRule(organization = "com.twitter")
    )

    val betterfiles = "com.github.pathikrit" %% "better-files" % version.betterfiles
  }

  object compiler {
    val macroParadise = "org.scalamacros" %% "paradise"       % "2.1.1" cross CrossVersion.full
    val kindProjector = "org.spire-math"  %% "kind-projector" % "0.9.4" cross CrossVersion.binary

    val plugins = Seq(macroParadise, kindProjector).map(compilerPlugin)
  }

  // Core module ===============================

  lazy val core = Def.settings {
    libraryDependencies ++= compiler.plugins ++ Seq(
      "org.typelevel"     %%% "cats-free"          % version.cats,
      "org.typelevel"     %%% "cats-effect"        % version.catsEffect,
      "io.circe"          %%% "circe-parser"       % version.circe,
      "io.circe"          %%% "circe-generic"      % version.circe,
      "io.circe"          %%% "circe-optics"       % version.circe,
      "io.circe"          %%% "circe-java8"        % version.circe,
      "io.circe"          %%% "circe-refined"      % version.circe,
      "com.beachape"      %%% "enumeratum"         % version.enumeratum,
      "com.beachape"      %%% "enumeratum-circe"   % version.enumCirce,
      "eu.timepit"        %%% "refined"            % version.refined,

      "com.github.julien-truffaut" %%% "monocle-core"  % version.monocle,
      "com.github.julien-truffaut" %%% "monocle-macro" % version.monocle,

      "com.github.alonsodomin.cron4s" %%% "cron4s-core" % version.cron4s
    )
  }

  lazy val coreJS = Def.settings {
    libraryDependencies ++= Seq(
      "io.circe"          %%% "circe-scalajs"   % version.circe,
      "io.github.cquiroz" %%% "scala-java-time" % version.scalaTime
    )
  }

  lazy val coreJVM = Def.settings {
    import libs._
    libraryDependencies ++= Seq(slf4j)
  }

  // Utilities module ===========================

  lazy val utilJS = Def.settings {
    jsDependencies ++= Seq(
      "org.webjars.npm" % "spark-md5" % version.sparkMD5
        /            "spark-md5.js"
        minified     "spark-md5.min.js"
        commonJSName "SparkMD5"
    )
  }

  // API module ===============================

  lazy val api = Def.settings(
    libraryDependencies ++= compiler.plugins ++ Seq(
      "io.suzaku" %%% "diode"          % version.diode,
      "io.monix"  %%% "monix-reactive" % version.monix
    )
  )

  // Client module ===============================

  lazy val client = Def.settings {
    libraryDependencies ++= compiler.plugins ++ Seq(
      "biz.enef" %%% "slogging" % version.slogging
    )
  }

  lazy val clientJS = Def.settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % version.scalaJsDom
    )
  )

  lazy val clientJVM = Def.settings {
    import libs._
    libraryDependencies ++= Log4j.All.map(_ % Test) ++ Seq(
      slogging_slf4j, Akka.actor, Akka.slf4j, Akka.clusterTools, Akka.clusterMetrics,
      Akka.kryo, Akka.http.main, Akka.http.sse,
      mockserver % Test
    )
  }

  // Console module ===============================

  lazy val console = Def.settings(
    libraryDependencies ++= compiler.plugins ++ Seq(
      "org.scalatest"    %%% "scalatest"      % version.scalaTest % Test,
      "io.suzaku"        %%% "diode-react"    % version.diode,
      "be.doeraene"      %%% "scalajs-jquery" % version.scalaJsJQuery,

      "com.github.japgolly.scalajs-react" %%% "core"         % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "extra"        % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-cats"     % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-monocle"  % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "test"         % version.scalaJsReact % Test,
      "com.github.japgolly.scalacss"      %%% "core"         % version.scalaCss,
      "com.github.japgolly.scalacss"      %%% "ext-react"    % version.scalaCss,

      "com.github.japgolly.test-state" %%% "core"              % version.testState % Test,
      "com.github.japgolly.test-state" %%% "dom-zipper"        % version.testState % Test,
      "com.github.japgolly.test-state" %%% "dom-zipper-sizzle" % version.testState % Test,
      "com.github.japgolly.test-state" %%% "ext-scalajs-react" % version.testState % Test,
      "com.github.japgolly.test-state" %%% "ext-cats"          % version.testState % Test
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
        dependsOn (s"${version.jquery}/jquery.js", "bootstrap.js"),

      // CodeMirror
      "org.webjars" % "codemirror" % version.codemirror
        /            "lib/codemirror.js"
        commonJSName "CodeMirror",
      "org.webjars" % "codemirror" % version.codemirror
        /         "mode/shell/shell.js"
        dependsOn "lib/codemirror.js",
      "org.webjars" % "codemirror" % version.codemirror
        /         "mode/python/python.js"
        dependsOn "lib/codemirror.js",
      "org.webjars" % "codemirror" % version.codemirror
        /         "addon/display/autorefresh.js"
        dependsOn "lib/codemirror.js",
      "org.webjars" % "codemirror" % version.codemirror
        /         "addon/edit/closebrackets.js"
        dependsOn "lib/codemirror.js",
      "org.webjars" % "codemirror" % version.codemirror
        /         "addon/edit/matchbrackets.js"
        dependsOn "lib/codemirror.js"
    )
  )

  // Server modules ===============================

  lazy val clusterShared = Def.settings {
    import libs._
    libraryDependencies ++= compiler.plugins ++ Log4j.All ++ Seq(
      Akka.actor, Akka.slf4j, Akka.clusterTools, Akka.clusterMetrics,
      Akka.kryo, ivy, scalaXml, pureconfig, slogging_slf4j,
      Kamon.core, Kamon.akka, Kamon.scala, Kamon.sysmetrics, Kamon.statsd,
      betterfiles,

      "eu.timepit" %% "refined-pureconfig" % version.refined
    )
  }

  lazy val clusterMaster = Def.settings {
    import libs._
    libraryDependencies ++= compiler.plugins ++ Seq(
      Akka.sharding, Akka.http.main, Akka.http.circe, Akka.http.sse,
      Akka.distributedData, Akka.persistence.core, Akka.persistence.cassandra,
      Akka.persistence.query, Akka.persistence.memory, Akka.constructr,
      Kamon.http, scopt, authenticatJwt,

      "com.vmunier"      %% "scalajs-scripts" % version.scalaJSScripts,
      "org.webjars"       % "codemirror"      % version.codemirror,
      "org.webjars.bower" % "animatewithsass" % version.animate
    )
  }

  lazy val clusterWorker = Def.settings {
    import libs._
    libraryDependencies ++= compiler.plugins ++ Seq(scopt, scalaArm)
  }

  // Support modules ================================

  lazy val testSupport = Def.settings {
    libraryDependencies ++= compiler.plugins ++ Seq(
      "io.github.cquiroz" %%% "scala-java-time"           % version.scalaTime,
      "org.scalatest"     %%% "scalatest"                 % version.scalaTest,
      "org.scalacheck"    %%% "scalacheck"                % version.scalaCheck,
      "org.typelevel"     %%% "cats-laws"                 % version.cats,
      "org.typelevel"     %%% "discipline"                % version.discipline,
      "biz.enef"          %%% "slogging"                  % version.slogging
    )
  }

  lazy val testSupportJVM = Def.settings {
    import libs._
    libraryDependencies ++= compiler.plugins ++ Log4j.All ++ Seq(
      slogging_slf4j, scalaMock, Akka.testKit, Akka.http.testkit
    )
  }

  // Examples modules ===============================

  lazy val exampleJobs = Def.settings {
    import libs._
    libraryDependencies ++= Log4j.All :+ slogging_slf4j
  }
  lazy val exampleProducers = Def.settings {
    import libs._
    libraryDependencies ++= Seq(Akka.testKit, scopt)
  }

}
