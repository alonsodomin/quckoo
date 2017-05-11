// Comment to get more information during initialization
logLevel := Level.Warn

// Project management plugins
addSbtPlugin("de.heikoseeberger" % "sbt-header"    % "2.0.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage" % "1.5.0")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"       % "1.0.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"   % "1.0.4")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"  % "1.1")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt"  % "0.5.4")

// Web plugins
addSbtPlugin("org.scala-js"     % "sbt-scalajs"     % "0.6.16")
addSbtPlugin("com.vmunier"      % "sbt-web-scalajs" % "1.0.4")
addSbtPlugin("org.irundaia.sbt" % "sbt-sassify"     % "1.4.8")
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl"       % "1.3.0")

// Server side plugins
addSbtPlugin("io.spray"         % "sbt-revolver"        % "0.8.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0-M8")
addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm"       % "0.3.11")
addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj"         % "0.10.6")
addSbtPlugin("com.tapad"        % "sbt-docker-compose"  % "1.0.22")

// Code generators
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
