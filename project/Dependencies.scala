import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

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
      val clusterTools    = "com.typesafe.akka" %% "akka-cluster-tools"     % version.akka
      val clusterMetrics  = "com.typesafe.akka" %% "akka-cluster-metrics"   % version.akka
      val sharding        = "com.typesafe.akka" %% "akka-cluster-sharding"  % version.akka
      val http            = "com.typesafe.akka" %% "akka-http-experimental" % version.akka
      val httpUpickle     = "de.heikoseeberger" %% "akka-http-upickle"      % "1.5.0"
      val sse             = "de.heikoseeberger" %% "akka-sse"               % "1.6.1"
      val distributedData = "com.typesafe.akka" %% "akka-distributed-data-experimental" % version.akka

      object persistence {
        val core      = "com.typesafe.akka"   %% "akka-persistence"              % version.akka
        val query     = "com.typesafe.akka"   %% "akka-persistence-query-experimental" % version.akka
        val cassandra = "com.typesafe.akka"   %% "akka-persistence-cassandra"    % "0.11"
        val jdbc      = "com.github.dnvriend" %% "akka-persistence-jdbc"         % "1.2.2"
        val memory    = "com.github.dnvriend" %% "akka-persistence-inmemory"     % "1.2.2" % Test
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
    object MonocleSJS {
      val core    = Def.setting { "com.github.japgolly.fork.monocle" %%% "monocle-core" % version.monocle }
      val `macro` = Def.setting { "com.github.japgolly.fork.monocle" %%% "monocle-macro" % version.monocle }
    }

    object ScalaJsReact {
      val core  = Def.setting { "com.github.japgolly.scalajs-react" %%% "core"  % version.scalaJsReact }
      val extra = Def.setting { "com.github.japgolly.scalajs-react" %%% "extra" % version.scalaJsReact }
      val test  = Def.setting { "com.github.japgolly.scalajs-react" %%% "test"  % version.scalaJsReact % Test }
    }

    object ScalaCss {
      val core  = Def.setting { "com.github.japgolly.scalacss" %%% "core"      % version.scalaCss }
      val react = Def.setting { "com.github.japgolly.scalacss" %%% "ext-react" % version.scalaCss }
    }

  }

  object compiler {
    val macroParadise = "org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full
  }

  object module {
    import libs._

    private[this] val akka = Seq(Akka.actor, Akka.clusterTools, Akka.clusterMetrics, Akka.testKit)
    private[this] val logging = Seq(slf4s, Log4j.api, Log4j.core, Log4j.slf4jImpl)
    private[this] val testing = Seq(scalaTest, scalaMock)

    val common   = testing :+ scalaXml
    val network  = logging ++ testing ++ akka
    val client   = logging ++ testing ++ akka
    val cluster  = logging ++ testing ++ akka ++ Seq(
      ivy, scalaXml, scalaz, mockito
    )

    val master = logging ++ testing ++ akka ++ Seq(
      Akka.persistence.core,
      Akka.persistence.cassandra,
      Akka.persistence.query,
      Akka.persistence.memory,
      Akka.sharding, Akka.http, Akka.httpUpickle, Akka.sse,
      Akka.distributedData, Akka.kryoSerialization,
      scopt, scalaz
    )

    val worker = logging ++ testing ++ akka ++ Seq(
      scopt, Akka.kryoSerialization
    )

    val exampleJobs = logging
    val exampleProducers = logging ++ testing ++ akka ++ Seq(
      scopt, Akka.kryoSerialization
    )

  }

}
