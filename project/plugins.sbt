// Comment to get more information during initialization
logLevel := Level.Warn

// Project management plugins
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.6.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.4.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "0.4.8")
addSbtPlugin("com.tapad" % "sbt-docker-compose" % "1.0.12")

// Web plugins
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.13")
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.2")
addSbtPlugin("org.madoushi.sbt" % "sbt-sass" % "0.9.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.1.1")

// Server side plugins
addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0-M6")
addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.11")

// Code generators
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")
