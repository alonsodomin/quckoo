import sbt._
import Keys._

object Dependencies {
  val akkaVersion = "2.3.11"
  val hazelcastVersion = "3.4.4"
  val sprayVersion = "1.2.0"

  val serverDeps: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
    "com.hazelcast" % "hazelcast" % hazelcastVersion,

    "io.spray" % "spray-can" % sprayVersion,
    "io.spray" % "spray-routing" % sprayVersion,
    "io.spray" % "spray-httpx" % sprayVersion,

    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "commons-io" % "commons-io" % "2.4" % "test"
  )

  val managerDeps: Seq[ModuleID] = Seq()

}