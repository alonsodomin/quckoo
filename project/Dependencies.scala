import sbt._

object Dependencies {
  val akkaVersion = "2.3.11"
  val hazelcastVersion = "3.4.4"
  val sprayVersion = "1.3.3"

  val serverDeps: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor"   % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-contrib" % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-slf4j"   % akkaVersion          withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test" withSources() withJavadoc(),

    "com.hazelcast" % "hazelcast" % hazelcastVersion,

    "io.spray" %% "spray-can"     % sprayVersion          withSources() withJavadoc(),
    "io.spray" %% "spray-routing" % sprayVersion          withSources() withJavadoc(),
    "io.spray" %% "spray-httpx"   % sprayVersion          withSources() withJavadoc(),
    "io.spray" %% "spray-testkit" % sprayVersion % "test" withSources() withJavadoc(),

    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "commons-io" % "commons-io" % "2.4" % "test"
  )

  val managerDeps: Seq[ModuleID] = Seq()

}