import sbt._
import Keys._

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Dependencies {

  // Common library definitions

  object libs {

    val scalaArm = "com.jsuereth"           %% "scala-arm" % Version.arm
    val scalaXml = "org.scala-lang.modules" %% "scala-xml" % Version.xml

    val ivy = "org.apache.ivy" % "ivy" % Version.ivy

    object Akka {
      val actor           = "com.typesafe.akka" %% "akka-actor-typed"            % Version.akka.main
      val slf4j           = "com.typesafe.akka" %% "akka-slf4j"                  % Version.akka.main
      val cluster         = "com.typesafe.akka" %% "akka-cluster-typed"          % Version.akka.main
      val clusterTools    = "com.typesafe.akka" %% "akka-cluster-tools"          % Version.akka.main
      val clusterMetrics  = "com.typesafe.akka" %% "akka-cluster-metrics"        % Version.akka.main
      val sharding        = "com.typesafe.akka" %% "akka-cluster-sharding-typed" % Version.akka.main
      val distributedData = "com.typesafe.akka" %% "akka-distributed-data"       % Version.akka.main

      object http {
        val main    = "com.typesafe.akka" %% "akka-http"         % Version.akka.http.main
        val testkit = "com.typesafe.akka" %% "akka-http-testkit" % Version.akka.http.main
        val circe   = "de.heikoseeberger" %% "akka-http-circe"   % Version.akka.http.json
        val sse     = "de.heikoseeberger" %% "akka-sse"          % Version.akka.http.sse
      }

      object persistence {
        val core      = "com.typesafe.akka"   %% "akka-persistence-typed"     % Version.akka.main
        val query     = "com.typesafe.akka"   %% "akka-persistence-query"     % Version.akka.main
        val cassandra = "com.typesafe.akka"   %% "akka-persistence-cassandra" % Version.akka.cassandra
        val memory    = "com.github.dnvriend" %% "akka-persistence-inmemory"  % Version.akka.inmemory % Test
      }

      val multiNodeTestKit = "com.typesafe.akka" %% "akka-multi-node-testkit"  % Version.akka.main
      val testKit          = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Version.akka.main

      val kryo       = "com.twitter" %% "chill-akka"                     % Version.akka.kryo
      val constructr = "com.tecsisa" %% "constructr-coordination-consul" % Version.akka.constructr % Runtime
    }

    object Log4j {
      val api       = "org.apache.logging.log4j" % "log4j-api"        % Version.log4j
      val core      = "org.apache.logging.log4j" % "log4j-core"       % Version.log4j
      val slf4jImpl = "org.apache.logging.log4j" % "log4j-slf4j-impl" % Version.log4j

      val All = Seq(api, core, slf4jImpl)
    }

    object Kamon {
      val core       = "io.kamon" %% "kamon-core"            % Version.kamon.core
      val akka       = "io.kamon" %% "kamon-akka-2.5"        % Version.kamon.akka
      val remote     = "io.kamon" %% "kamon-akka-remote-2.5" % Version.kamon.remote
      val http       = "io.kamon" %% "kamon-akka-http-2.5"   % Version.kamon.http
      val scala      = "io.kamon" %% "kamon-scala-future"    % Version.kamon.scala
      val sysmetrics = "io.kamon" %% "kamon-system-metrics"  % Version.kamon.sysmetrics
      val prometheus = "io.kamon" %% "kamon-prometheus"      % Version.kamon.prometheus

      lazy val All = Seq(core, akka, http, scala, sysmetrics, prometheus)
    }

    object Pureconfig {
      val core       = "com.github.pureconfig" %% "pureconfig"            % Version.pureconfig
      val cats       = "com.github.pureconfig" %% "pureconfig-cats"       % Version.pureconfig
      val enumeratum = "com.github.pureconfig" %% "pureconfig-enumeratum" % Version.pureconfig
      val akka       = "com.github.pureconfig" %% "pureconfig-akka"       % Version.pureconfig

      lazy val All = Seq(core, enumeratum, cats, akka)
    }

    val slf4j          = "org.slf4j" % "slf4j-api"       % Version.slf4j
    val slogging_slf4j = "biz.enef"  %% "slogging-slf4j" % Version.slogging

    val scopt = "com.github.scopt" %% "scopt" % Version.scopt

    val authenticatJwt = "com.jason-goodwin" %% "authentikat-jwt" % "0.4.5"

    val scalaTest = "org.scalatest"          %% "scalatest"                   % Version.scalaTest
    val scalaMock = "org.scalamock"          %% "scalamock-scalatest-support" % Version.scalaMock
    val wiremock  = "com.github.tomakehurst" % "wiremock"                     % Version.wiremock

    val betterfiles = "com.github.pathikrit" %% "better-files" % Version.betterfiles
  }

  object compiler {
    val macroParadise = "org.scalamacros" %% "paradise"           % "2.1.1" cross CrossVersion.full
    val kindProjector = "org.typelevel"   %% "kind-projector"     % "0.10.3" cross CrossVersion.binary
    val monadicFor    = "com.olegpy"      %% "better-monadic-for" % "0.3.1"

    val plugins = Seq(macroParadise, kindProjector, monadicFor).map(compilerPlugin)
  }

  // Core module ===============================

  lazy val core = Def.settings {
    libraryDependencies ++= Seq(
      "org.typelevel"                 %%% "cats-free"        % Version.cats.main,
      "org.typelevel"                 %%% "cats-mtl-core"    % Version.cats.mtl,
      "org.typelevel"                 %%% "cats-effect"      % Version.cats.effect,
      "org.typelevel"                 %%% "kittens"          % Version.cats.kittens,
      "io.circe"                      %%% "circe-parser"     % Version.circe,
      "io.circe"                      %%% "circe-generic"    % Version.circe,
      "io.circe"                      %%% "circe-refined"    % Version.circe,
      "com.beachape"                  %%% "enumeratum-circe" % Version.enumeratum,
      "eu.timepit"                    %%% "refined"          % Version.refined,
      "eu.timepit"                    %%% "refined-cats"     % Version.refined,
      "com.github.julien-truffaut"    %%% "monocle-core"     % Version.monocle,
      "com.github.julien-truffaut"    %%% "monocle-macro"    % Version.monocle,
      "com.github.alonsodomin.cron4s" %%% "cron4s-core"      % Version.cron4s,
      "com.github.alonsodomin.cron4s" %%% "cron4s-circe"     % Version.cron4s
    )
  }

  lazy val coreJS = Def.settings {
    libraryDependencies ++= Seq(
      "io.circe"          %%% "circe-scalajs"   % Version.circe,
      "io.github.cquiroz" %%% "scala-java-time" % Version.scalaTime
    )
  }

  lazy val coreJVM = Def.settings {
    import libs._
    libraryDependencies ++= Seq(slf4j)
  }

  // Utilities module ===========================

  lazy val utilJS = Def.settings {
    /* jsDependencies ++= Seq(
      "org.webjars.npm" % "spark-md5" % Version.sparkMD5
        /            "spark-md5.js"
        minified     "spark-md5.min.js"
        commonJSName "SparkMD5"
    ),*/
    npmDependencies in Compile += "spark-md5" -> Version.sparkMD5
  }

  // API module ===============================

  lazy val api = Def.settings(
    libraryDependencies ++= Seq(
      "io.suzaku" %%% "diode"          % Version.diode.core,
      "io.monix"  %%% "monix-reactive" % Version.monix
    )
  )

  // Client module ===============================

  lazy val client = Def.settings {
    libraryDependencies ++= Seq(
      "biz.enef"              %%% "slogging" % Version.slogging,
      "com.softwaremill.sttp" %%% "core"     % Version.sttp,
      "com.softwaremill.sttp" %%% "circe"    % Version.sttp
    )
  }

  lazy val clientJS = Def.settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp" %%% "monix"       % Version.sttp,
      "org.scala-js"          %%% "scalajs-dom" % Version.scalaJsDom
    )
  )

  lazy val clientJVM = Def.settings {
    import libs._
    libraryDependencies ++= Log4j.All.map(_ % Runtime) ++ Seq(
      "com.softwaremill.sttp"               %% "async-http-client-backend-monix" % Version.sttp,
      slogging_slf4j,
      Akka.actor,
      Akka.slf4j,
      Akka.cluster,
      Akka.clusterTools,
      Akka.clusterMetrics,
      Akka.kryo,
      Akka.http.main,
      Akka.http.sse,
      wiremock % Test
    )
  }

  // Console module ===============================

  lazy val console = Def.settings(
    libraryDependencies ++= Seq(
      "org.scalatest"                     %%% "scalatest"         % Version.scalaTest % Test,
      "io.suzaku"                         %%% "diode-react"       % Version.diode.react,
      "be.doeraene"                       %%% "scalajs-jquery"    % Version.scalaJsJQuery,
      "com.github.japgolly.scalajs-react" %%% "core"              % Version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "extra"             % Version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-cats"          % Version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-monocle-cats"  % Version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "test"              % Version.scalaJsReact % Test,
      "com.github.japgolly.scalacss"      %%% "core"              % Version.scalaCss,
      "com.github.japgolly.scalacss"      %%% "ext-react"         % Version.scalaCss,
      "com.github.japgolly.test-state"    %%% "core"              % Version.testState % Test,
      "com.github.japgolly.test-state"    %%% "dom-zipper"        % Version.testState % Test,
      "com.github.japgolly.test-state"    %%% "dom-zipper-sizzle" % Version.testState % Test,
      "com.github.japgolly.test-state"    %%% "ext-scalajs-react" % Version.testState % Test,
      "com.github.japgolly.test-state"    %%% "ext-cats"          % Version.testState % Test
    ),
    npmDependencies in Compile ++= Seq(
      "react"            -> Version.reactJs,
      "react-dom"        -> Version.reactJs,
      "jquery"           -> Version.jquery,
      "bootstrap"        -> Version.bootstrap,
      "bootstrap-notify" -> Version.bootstrapNotifiy,
      "codemirror"       -> Version.codemirror
    )
    /*jsDependencies ++= Seq(
      //"org.webjars.npm" % "js-tokens" % "4.0.0" / "index.js",

      // -- ReactJS
      "org.webjars.npm" % "react" % Version.reactJs
         /        "umd/react.development.js"
         minified "umd/react.production.min.js"
         commonJSName "React",
      "org.webjars.npm" % "react-dom" % Version.reactJs
         /         "umd/react-dom.development.js"
         minified  "umd/react-dom.production.min.js"
         dependsOn "umd/react.development.js"
         commonJSName "ReactDOM",
      "org.webjars.npm" % "react-dom" % Version.reactJs
         /         "umd/react-dom-server.browser.development.js"
         minified  "umd/react-dom-server.browser.production.min.js"
         dependsOn "umd/react-dom.development.js"
         commonJSName "ReactDOMServer",

      // -- JQuery & Bootstrap
      "org.webjars" % "jquery"    % Version.jquery
         /        s"${Version.jquery}/jquery.js"
         minified "jquery.min.js",
      "org.webjars" % "bootstrap" % Version.bootstrap
         /         "bootstrap.js"
         minified  "bootstrap.min.js"
         dependsOn s"${Version.jquery}/jquery.js",
      "org.webjars" % "bootstrap-notify" % Version.bootstrapNotifiy
         /         "bootstrap-notify.js"
         minified  "bootstrap-notify.min.js"
         dependsOn (s"${Version.jquery}/jquery.js", "bootstrap.js"),

      // -- CodeMirror
      "org.webjars" % "codemirror" % Version.codemirror
         /            "lib/codemirror.js"
         commonJSName "CodeMirror",
      "org.webjars" % "codemirror" % Version.codemirror
         /         "mode/shell/shell.js"
         dependsOn "lib/codemirror.js",
      "org.webjars" % "codemirror" % Version.codemirror
         /         "mode/python/python.js"
         dependsOn "lib/codemirror.js",
      "org.webjars" % "codemirror" % Version.codemirror
         /         "addon/display/autorefresh.js"
         dependsOn "lib/codemirror.js",
      "org.webjars" % "codemirror" % Version.codemirror
         /         "addon/edit/closebrackets.js"
         dependsOn "lib/codemirror.js",
      "org.webjars" % "codemirror" % Version.codemirror
         /         "addon/edit/matchbrackets.js"
         dependsOn "lib/codemirror.js"
    )*/
  )

  // Server modules ===============================

  lazy val clusterShared = Def.settings {
    import libs._
    libraryDependencies ++= Kamon.All ++ Log4j.All ++ Pureconfig.All ++ Seq(
      Akka.actor,
      Akka.slf4j,
      Akka.cluster,
      Akka.clusterTools,
      Akka.clusterMetrics,
      Akka.kryo,
      ivy,
      scalaXml,
      slogging_slf4j,
      betterfiles,
      "eu.timepit" %% "refined-pureconfig" % Version.refined
    )
  }

  lazy val clusterMaster = Def.settings {
    import libs._
    libraryDependencies ++= Seq(
      Akka.sharding,
      Akka.http.main,
      Akka.http.circe,
      Akka.http.sse,
      Akka.distributedData,
      Akka.persistence.core,
      Akka.persistence.cassandra,
      Akka.persistence.query,
      Akka.persistence.memory,
      Kamon.http,
      scopt,
      authenticatJwt,
      "com.vmunier"       %% "scalajs-scripts" % Version.scalaJSScripts,
      "org.webjars"       % "codemirror"       % Version.codemirror,
      "org.webjars.bower" % "animatewithsass"  % Version.animate
    )
  }

  lazy val clusterWorker = Def.settings {
    import libs._
    libraryDependencies ++= Seq(scopt, scalaArm)
  }

  // Support modules ================================

  lazy val testkit = Def.settings {
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time"    % Version.scalaTime,
      "org.scalatest"     %%% "scalatest"          % Version.scalaTest,
      "org.scalacheck"    %%% "scalacheck"         % Version.scalaCheck,
      "org.typelevel"     %%% "cats-laws"          % Version.cats.main,
      "org.typelevel"     %%% "discipline"         % Version.discipline,
      "biz.enef"          %%% "slogging"           % Version.slogging,
      "eu.timepit"        %%% "refined-scalacheck" % Version.refined
    )
  }

  lazy val testkitJVM = Def.settings {
    import libs._
    libraryDependencies ++= Log4j.All ++ Seq(
      slogging_slf4j,
      scalaMock,
      Akka.testKit,
      Akka.http.testkit
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
