import sbt._
import Keys._

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._

object Dependencies {

  object version {

    // Logging -------

    val slogging = "0.6.1"
    val log4j    = "2.11.1"
    val slf4j    = "1.7.25"

    // Testing --------

    val scalaTest  = "3.0.5"
    val scalaCheck = "1.13.5"
    val scalaMock  = "3.6.0"
    val discipline = "0.10.0"
    val wiremock   = "2.18.0"

    // Akka ----------

    object akka {
      val main = "2.5.14"
      val kryo = "0.9.2"

      val constructr = "0.9.0"

      object http {
        val main = "10.1.3"

        // http extensions
        val json = "1.21.0"
        val sse  = "3.0.0"
      }

      // persistence plugins
      val cassandra = "0.87"
      val inmemory  = "2.5.1.1"
    }

    // Monitoring ----

    object kamon {
      val core       = "1.1.3"
      val akka       = "1.1.2"
      val remote     = "1.0.1"
      val http       = "1.1.0"
      val scala      = "1.0.0"
      val sysmetrics = "1.0.0"
      val prometheus = "1.1.1"
    }

    // ScalaJS -------

    val scalaJsReact    = "1.2.3"
    val scalaJsDom      = "0.9.6"
    val scalaJsJQuery   = "0.9.4"
    val scalaJSScripts  = "1.1.2"
    val testState       = "2.2.0"

    // Other utils ---

    val arm         = "2.0"
    val betterfiles = "3.6.0"
    object diode {
      val core  = "1.1.3"
      val react = s"$core.120"
    }
    object cats {
      val main      = "1.1.0"
      val mtl       = "0.3.0"
      val effect    = "1.0.0-RC2"
      val kittens   = "1.1.0"
    }
    val circe       = "0.9.3"
    val cron4s      = "0.4.5"
    val enumeratum  = "1.5.17"
    val ivy         = "2.4.0"
    val monix       = "3.0.0-RC1"
    val monocle     = "1.5.1-cats"
    val pureconfig  = "0.9.1"
    val refined     = "0.9.1"
    val scalaCss    = "0.5.5"
    val scalafmt    = "1.4.0"
    val scalaTime   = "2.0.0-M13"
    val scopt       = "3.7.0"
    val sttp        = "1.2.3"
    val xml         = "1.1.0"

    // JavaScript Libraries

    val animate          = "3.2.2"
    val jquery           = "2.2.4"
    val bootstrap        = "3.3.5"
    val bootstrapNotifiy = "3.1.3"
    val reactJs          = "16.4.1"
    val sparkMD5         = "3.0.0"
    val codemirror       = "5.33.0"
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
      val akka       = "io.kamon" %% "kamon-akka-2.5"        % version.kamon.akka
      val remote     = "io.kamon" %% "kamon-akka-remote-2.5" % version.kamon.remote
      val http       = "io.kamon" %% "kamon-akka-http-2.5"   % version.kamon.http
      val scala      = "io.kamon" %% "kamon-scala-future"    % version.kamon.scala
      val sysmetrics = "io.kamon" %% "kamon-system-metrics"  % version.kamon.sysmetrics
      val prometheus = "io.kamon" %% "kamon-prometheus"      % version.kamon.prometheus

      lazy val All = Seq(core, akka, http, scala, sysmetrics, prometheus)
    }

    object Pureconfig {
      val core       = "com.github.pureconfig" %% "pureconfig"            % version.pureconfig
      val cats       = "com.github.pureconfig" %% "pureconfig-cats"       % version.pureconfig
      val enumeratum = "com.github.pureconfig" %% "pureconfig-enumeratum" % version.pureconfig
      val akka       = "com.github.pureconfig" %% "pureconfig-akka"       % version.pureconfig

      lazy val All = Seq(core, enumeratum, cats, akka)
    }

    val slf4j          = "org.slf4j" % "slf4j-api"      % version.slf4j
    val slogging_slf4j = "biz.enef" %% "slogging-slf4j" % version.slogging

    val scopt = "com.github.scopt" %% "scopt" % version.scopt

    val authenticatJwt = "com.jason-goodwin"     %% "authentikat-jwt" % "0.4.5"

    val scalaTest  = "org.scalatest"   %% "scalatest"                   % version.scalaTest
    val scalaMock  = "org.scalamock"   %% "scalamock-scalatest-support" % version.scalaMock
    val wiremock   = "com.github.tomakehurst" % "wiremock" % version.wiremock

    val betterfiles = "com.github.pathikrit" %% "better-files" % version.betterfiles
  }

  object compiler {
    val macroParadise = "org.scalamacros" %% "paradise"       % "2.1.1" cross CrossVersion.full
    val kindProjector = "org.spire-math"  %% "kind-projector" % "0.9.7" cross CrossVersion.binary

    val plugins = Seq(macroParadise, kindProjector).map(compilerPlugin)
  }

  // Core module ===============================

  lazy val core = Def.settings {
    libraryDependencies ++= compiler.plugins ++ Seq(
      "org.typelevel"     %%% "cats-free"          % version.cats.main,
      "org.typelevel"     %%% "cats-mtl-core"      % version.cats.mtl,
      "org.typelevel"     %%% "cats-effect"        % version.cats.effect,
      "org.typelevel"     %%% "kittens"            % version.cats.kittens,
      "io.circe"          %%% "circe-parser"       % version.circe,
      "io.circe"          %%% "circe-generic"      % version.circe,
      //"io.circe"          %%% "circe-optics"       % version.circe,
      "io.circe"          %%% "circe-java8"        % version.circe,
      "io.circe"          %%% "circe-refined"      % version.circe,
      "com.beachape"      %%% "enumeratum-circe"   % version.enumeratum,
      "eu.timepit"        %%% "refined"            % version.refined,
      "eu.timepit"        %%% "refined-cats"       % version.refined,

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

  lazy val utilJS = Def.settings(
    npmDependencies in Compile ++= Seq(
      "spark-md5" -> version.sparkMD5
    )
  )

  // API module ===============================

  lazy val api = Def.settings(
    libraryDependencies ++= compiler.plugins ++ Seq(
      "io.suzaku" %%% "diode"          % version.diode.core,
      "io.monix"  %%% "monix-reactive" % version.monix
    )
  )

  // Client module ===============================

  lazy val client = Def.settings {
    libraryDependencies ++= compiler.plugins ++ Seq(
      "biz.enef"              %%% "slogging" % version.slogging,
      "com.softwaremill.sttp" %%% "core"     % version.sttp,
      "com.softwaremill.sttp" %%% "circe"    % version.sttp
    )
  }

  lazy val clientJS = Def.settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp" %%% "monix"       % version.sttp,
      "org.scala-js"          %%% "scalajs-dom" % version.scalaJsDom
    )
  )

  lazy val clientJVM = Def.settings {
    import libs._
    libraryDependencies ++= Log4j.All.map(_ % Runtime) ++ Seq(
      "com.softwaremill.sttp" %% "async-http-client-backend-monix"     % version.sttp,
      slogging_slf4j, Akka.actor, Akka.slf4j, Akka.clusterTools, Akka.clusterMetrics,
      Akka.kryo, Akka.http.main, Akka.http.sse,
      wiremock % Test
    )
  }

  // Console module ===============================

  lazy val console = Def.settings(
    libraryDependencies ++= compiler.plugins ++ Seq(
      "org.scalatest"    %%% "scalatest"      % version.scalaTest % Test,
      "io.suzaku"        %%% "diode-react"    % version.diode.react,
      "be.doeraene"      %%% "scalajs-jquery" % version.scalaJsJQuery,

      "com.github.japgolly.scalajs-react" %%% "core"             % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "extra"            % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-cats"         % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-monocle-cats" % version.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "test"             % version.scalaJsReact % Test,
      "com.github.japgolly.scalacss"      %%% "core"             % version.scalaCss,
      "com.github.japgolly.scalacss"      %%% "ext-react"        % version.scalaCss,

      "com.github.japgolly.test-state" %%% "core"              % version.testState % Test,
      "com.github.japgolly.test-state" %%% "dom-zipper"        % version.testState % Test,
      "com.github.japgolly.test-state" %%% "dom-zipper-sizzle" % version.testState % Test,
      "com.github.japgolly.test-state" %%% "ext-scalajs-react" % version.testState % Test,
      "com.github.japgolly.test-state" %%% "ext-cats"          % version.testState % Test
    ),
    npmDependencies in Compile ++= Seq(
      "react"            -> version.reactJs,
      "react-dom"        -> version.reactJs,
      "jquery"           -> version.jquery,
      "bootstrap3"       -> version.bootstrap,
      "bootstrap-notify" -> version.bootstrapNotifiy,
      "codemirror"       -> version.codemirror
    )
    //jsDependencies ++= Seq(
    // ReactJS
    //"org.webjars.npm" % "react" % version.reactJs
    //  /        "umd/react.development.js"
    //  minified "umd/react.production.min.js"
    //  commonJSName "React",
    //"org.webjars.npm" % "react-dom" % version.reactJs
    //  /         "umd/react-dom.development.js"
    //  minified  "umd/react-dom.production.min.js"
    //  dependsOn "umd/react.development.js"
    //  commonJSName "ReactDOM",
    //"org.webjars.npm" % "react-dom" % version.reactJs
    //  /         "umd/react-dom-server.browser.development.js"
    //  minified  "umd/react-dom-server.browser.production.min.js"
    //  dependsOn "umd/react-dom.development.js"
    //  commonJSName "ReactDOMServer",

    // JQuery & Bootstrap
    //"org.webjars" % "jquery"    % version.jquery
    //  /        s"${version.jquery}/jquery.js"
    //  minified "jquery.min.js",
    //"org.webjars" % "bootstrap" % version.bootstrap
    //  /         "bootstrap.js"
    //  minified  "bootstrap.min.js"
    //  dependsOn s"${version.jquery}/jquery.js",
    //"org.webjars" % "bootstrap-notify" % version.bootstrapNotifiy
    //  /         "bootstrap-notify.js"
    //  minified  "bootstrap-notify.min.js"
    //  dependsOn (s"${version.jquery}/jquery.js", "bootstrap.js"),

    // CodeMirror
    //"org.webjars" % "codemirror" % version.codemirror
    //  /            "lib/codemirror.js"
    //  commonJSName "CodeMirror",
    //"org.webjars" % "codemirror" % version.codemirror
    //  /         "mode/shell/shell.js"
    //  dependsOn "lib/codemirror.js",
    //"org.webjars" % "codemirror" % version.codemirror
    //  /         "mode/python/python.js"
    //  dependsOn "lib/codemirror.js",
    //"org.webjars" % "codemirror" % version.codemirror
    //  /         "addon/display/autorefresh.js"
    //  dependsOn "lib/codemirror.js",
    //"org.webjars" % "codemirror" % version.codemirror
    //  /         "addon/edit/closebrackets.js"
    //  dependsOn "lib/codemirror.js",
    //"org.webjars" % "codemirror" % version.codemirror
    //  /         "addon/edit/matchbrackets.js"
    //  dependsOn "lib/codemirror.js"
    //)
  )

  // Server modules ===============================

  lazy val clusterShared = Def.settings {
    import libs._
    libraryDependencies ++= compiler.plugins ++ Kamon.All ++ Log4j.All ++ Pureconfig.All ++ Seq(
      Akka.actor, Akka.slf4j, Akka.clusterTools, Akka.clusterMetrics,
      Akka.kryo, ivy, scalaXml, slogging_slf4j, betterfiles,

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
      "io.github.cquiroz" %%% "scala-java-time"    % version.scalaTime,
      "org.scalatest"     %%% "scalatest"          % version.scalaTest,
      "org.scalacheck"    %%% "scalacheck"         % version.scalaCheck,
      "org.typelevel"     %%% "cats-laws"          % version.cats.main,
      "org.typelevel"     %%% "discipline"         % version.discipline,
      "biz.enef"          %%% "slogging"           % version.slogging,
      "eu.timepit"        %%% "refined-scalacheck" % version.refined
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
